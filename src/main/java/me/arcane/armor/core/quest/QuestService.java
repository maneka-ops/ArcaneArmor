package me.arcane.armor.core.quest;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.api.armor.ArmorSlot;
import me.arcane.armor.api.registry.Registry;
import me.arcane.armor.config.ArcaneConfig;
import me.arcane.armor.config.Messages;
import me.arcane.armor.core.armor.ArmorState;
import me.arcane.armor.core.armor.EquipmentService;
import me.arcane.armor.core.cosmetic.CosmeticService;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Armor quests: what each player has to do before the Arcane Forge will make a set for them.
 *
 * <p>Quests follow the set chain. A set's quest only starts counting once the quest for the set
 * below it is done, so a new player works Apprentice, then Storm, then Infernal, and one kill never
 * advances two rungs at once. A set with no quest configured is always unlocked.
 *
 * <p>Progress on a set's quest also only counts while the player is wearing the set below it — the
 * Storm quest is done in Apprentice armor, the Infernal quest in Storm armor — so each quest is a
 * test of the armor the player already has.
 *
 * <p>Objectives can be limited to a dimension or to night time, delivery objectives are handed in
 * from the quest menu, dying costs a share of unfinished progress, and finishing a quest can grant
 * experience and run reward commands.
 *
 * <p>The Forge asks {@link #isUnlocked} from Slimefun's async ticker, so unlock state lives in
 * concurrent collections and is loaded for every player at startup — the Forge's owner is usually
 * offline when it ticks. Progress counters are only ever written from the main thread.
 */
public final class QuestService {

    private static final String DATA_FILE = "quest-data.yml";

    /** How a quest currently stands for one player. */
    public enum Status {
        /** Done; the set can be forged. */
        COMPLETE,
        /** Counting progress now. */
        ACTIVE,
        /** Waiting on the previous set's quest. */
        LOCKED
    }

    private final Plugin plugin;
    private final ArcaneConfig config;
    private final Messages messages;
    private final Registry<ArmorSetDefinition> sets;
    private final EquipmentService equipmentService;
    private final CosmeticService cosmeticService;
    private final File dataFile;
    private final Object writeLock = new Object();

    private final Map<UUID, PlayerQuestData> players = new ConcurrentHashMap<>();
    private volatile Map<String, ArmorQuest> quests = Map.of();
    private volatile boolean dirty;

    /** Set id to the name of the first player to forge a full set of it, persisted across restarts. */
    private final Map<String, String> firstForged = new ConcurrentHashMap<>();

    public QuestService(
            @Nonnull Plugin plugin,
            @Nonnull ArcaneConfig config,
            @Nonnull Messages messages,
            @Nonnull Registry<ArmorSetDefinition> sets,
            @Nonnull EquipmentService equipmentService,
            @Nonnull CosmeticService cosmeticService) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.sets = sets;
        this.equipmentService = equipmentService;
        this.cosmeticService = cosmeticService;
        this.dataFile = new File(plugin.getDataFolder(), DATA_FILE);
    }

    // ---- Definitions -------------------------------------------------------------------------

    /**
     * Re-reads every quest from {@code quests.sets} in the config. Called at startup and from
     * {@code /arcane reload}; bad entries are logged and skipped rather than failing the whole file.
     */
    public void reloadDefinitions() {
        Map<String, ArmorQuest> parsed = new HashMap<>();
        ConfigurationSection section = config.raw().getConfigurationSection("quests.sets");

        if (section != null) {
            for (String rawId : section.getKeys(false)) {
                String setId = rawId.toLowerCase(Locale.ROOT);
                List<QuestObjective> objectives = new ArrayList<>();

                for (Map<?, ?> entry : section.getMapList(rawId + ".objectives")) {
                    QuestObjective objective = parseObjective(setId, entry);
                    if (objective != null) {
                        objectives.add(objective);
                    }
                }

                if (!objectives.isEmpty()) {
                    parsed.put(setId, new ArmorQuest(setId, objectives,
                            section.getInt(rawId + ".rewards.levels", 0),
                            section.getStringList(rawId + ".rewards.commands")));
                }
            }
        }

        quests = Map.copyOf(parsed);
    }

    @Nullable
    private QuestObjective parseObjective(@Nonnull String setId, @Nonnull Map<?, ?> entry) {
        QuestObjective.Type type;
        try {
            type = QuestObjective.Type.valueOf(String.valueOf(entry.get("type")).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            warn(setId, "unknown objective type '" + entry.get("type") + "', expected KILL, MINE, ABILITY or DELIVER");
            return null;
        }

        Object rawTargets = entry.get("target");
        List<String> names = rawTargets instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : rawTargets == null ? List.of() : List.of(String.valueOf(rawTargets));

        Set<String> targets = new HashSet<>();
        for (String name : names) {
            String upper = name.toUpperCase(Locale.ROOT);
            if (isValidTarget(type, upper)) {
                targets.add(upper);
            } else {
                warn(setId, "'" + name + "' is not a valid target for a " + type + " objective");
            }
        }

        if (targets.isEmpty()) {
            warn(setId, "objective has no valid targets and was skipped");
            return null;
        }

        World.Environment dimension = null;
        if (entry.get("dimension") != null) {
            String raw = String.valueOf(entry.get("dimension")).toUpperCase(Locale.ROOT);
            dimension = switch (raw) {
                case "OVERWORLD", "NORMAL" -> World.Environment.NORMAL;
                case "NETHER" -> World.Environment.NETHER;
                case "END", "THE_END" -> World.Environment.THE_END;
                default -> {
                    warn(setId, "unknown dimension '" + raw + "', expected OVERWORLD, NETHER or END");
                    yield null;
                }
            };
        }

        int amount = entry.get("amount") instanceof Number number ? number.intValue() : 1;
        String label = entry.get("name") != null ? String.valueOf(entry.get("name")) : prettify(names.get(0));
        boolean night = Boolean.TRUE.equals(entry.get("night"));
        return new QuestObjective(type, targets, amount, label, dimension, night);
    }

    private static boolean isValidTarget(@Nonnull QuestObjective.Type type, @Nonnull String name) {
        return switch (type) {
            case MINE -> {
                Material material = Material.matchMaterial(name);
                yield material != null && material.isBlock();
            }
            case KILL -> {
                try {
                    EntityType.valueOf(name);
                    yield true;
                } catch (IllegalArgumentException ex) {
                    yield false;
                }
            }
            // Ability ids are checked loosely: addons can register abilities after quests load.
            case ABILITY -> !name.isBlank();
            case DELIVER -> {
                if (SlimefunItem.getById(name) != null) {
                    yield true;
                }
                Material material = Material.matchMaterial(name);
                yield material != null && material.isItem();
            }
        };
    }

    /** Id a delivered stack is matched by: its Slimefun id, or its vanilla material for plain items. */
    @Nonnull
    private static String deliveryId(@Nonnull ItemStack stack) {
        SlimefunItem slimefunItem = SlimefunItem.getByItem(stack);
        return slimefunItem != null ? slimefunItem.getId() : stack.getType().name();
    }

    private void warn(@Nonnull String setId, @Nonnull String problem) {
        plugin.getLogger().warning("Quest for set '" + setId + "': " + problem);
    }

    // ---- Queries (any thread) ----------------------------------------------------------------

    public boolean isEnabled() {
        return config.raw().getBoolean("quests.enabled", true);
    }

    @Nullable
    public ArmorQuest getQuest(@Nonnull String setId) {
        return quests.get(setId);
    }

    /** Whether this player may forge the set. Always true when quests are off or the set has none. */
    public boolean isUnlocked(@Nonnull UUID player, @Nonnull String setId) {
        if (!isEnabled() || !quests.containsKey(setId)) {
            return true;
        }
        PlayerQuestData data = players.get(player);
        return data != null && data.unlocked.contains(setId);
    }

    /** Whether this player has already forged an armor piece for this set and slot. */
    public boolean hasCraftedArmor(@Nonnull UUID player, @Nonnull String setId, @Nonnull ArmorSlot slot) {
        PlayerQuestData data = players.get(player);
        if (data == null) {
            return false;
        }
        Set<String> slots = data.crafted.get(setId);
        return slots != null && slots.contains(slot.name());
    }

    /** Records that a player has forged an armor piece for this set and slot. */
    public void recordArmorCrafted(@Nonnull UUID player, @Nonnull String setId, @Nonnull ArmorSlot slot) {
        PlayerQuestData data = players.computeIfAbsent(player, k -> new PlayerQuestData());
        Set<String> slots = data.crafted.computeIfAbsent(setId, k -> ConcurrentHashMap.newKeySet());

        if (!slots.add(slot.name())) {
            return;
        }
        dirty = true;

        ArmorSetDefinition set = sets.get(setId);
        if (set != null && config.isPieceForgedBroadcast()) {
            announcePieceForged(player, set, slot);
        }

        // A player has finished a whole set when they have forged every slot once. The slot limit
        // means this can only ever fire once per player per set, so no extra "announced" flag is
        // needed to avoid a repeat.
        if (slots.size() >= ArmorSlot.values().length) {
            maybeAnnounceWorldFirst(player, setId);
        }
    }

    /** How many pieces of this set the player has forged (0 to 4). */
    public int getCraftedCount(@Nonnull UUID player, @Nonnull String setId) {
        PlayerQuestData data = players.get(player);
        if (data == null) {
            return 0;
        }
        Set<String> slots = data.crafted.get(setId);
        return slots != null ? slots.size() : 0;
    }

    /**
     * Announces the first player ever to forge a complete set.
     *
     * <p>The claim on "first" is taken immediately, on whatever thread the Forge ticker runs on,
     * so two players finishing the last two pieces in the same tick cannot both win it. The chat
     * message itself is sent from the main thread, because the machines tick async.
     */
    private void maybeAnnounceWorldFirst(@Nonnull UUID playerId, @Nonnull String setId) {
        if (!config.isWorldFirstBroadcast() || firstForged.containsKey(setId)) {
            return;
        }

        ArmorSetDefinition set = sets.get(setId);
        String crafter = resolveName(playerId);
        if (set == null || firstForged.putIfAbsent(setId, crafter) != null) {
            return;
        }
        dirty = true;

        Map<String, String> placeholders = Map.of(
                "player", crafter,
                "set", legacyColor(set) + set.getDisplayName());

        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getServer().sendMessage(messages.get("world-first",
                    "&6&lWorld First! &e{player} &7is the first player to forge a full {set} &7set!",
                    placeholders));
            if (config.isSoundsEnabled()) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.playSound(online.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.2F);
                }
            }

            Player crafterEntity = Bukkit.getPlayer(playerId);
            if (crafterEntity != null) {
                cosmeticService.worldFirstBurst(crafterEntity, set);
            }
        });
    }

    /** The player's name, falling back to offline lookup and then to something readable. */
    @Nonnull
    private String resolveName(@Nonnull UUID playerId) {
        Player online = Bukkit.getPlayer(playerId);
        if (online != null) {
            return online.getName();
        }
        String name = Bukkit.getOfflinePlayer(playerId).getName();
        return name == null || name.isBlank() ? "an unnamed craftsman" : name;
    }

    /**
     * Tells everyone that a piece of armor was just forged.
     *
     * <p>Because each player can forge a given piece only once, this is a rare event — it is a
     * status update about the quest ladder rather than noise. Sent from the main thread, like the
     * world-first, because the Forge ticks async.
     */
    private void announcePieceForged(
            @Nonnull UUID playerId,
            @Nonnull ArmorSetDefinition set,
            @Nonnull ArmorSlot slot) {
        String crafter = resolveName(playerId);
        Map<String, String> placeholders = Map.of(
                "player", crafter,
                "piece", slot.getDisplayName(),
                "set", legacyColor(set) + set.getDisplayName());

        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getServer().sendMessage(messages.get("piece-forged",
                    "&e{player} &7forged a {set} &e{piece} &7at the Arcane Forge!",
                    placeholders));
            if (config.isSoundsEnabled()) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.playSound(online.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 0.4F, 1.4F);
                }
            }

            Player crafterEntity = Bukkit.getPlayer(playerId);
            if (crafterEntity != null) {
                cosmeticService.forgeBurst(crafterEntity, set);
            }
        });
    }

    @Nonnull
    public Status getStatus(@Nonnull UUID player, @Nonnull ArmorSetDefinition set) {
        if (isUnlocked(player, set.getId())) {
            return Status.COMPLETE;
        }
        String previous = set.getPreviousSetId();
        if (previous != null && sets.contains(previous) && !isUnlocked(player, previous)) {
            return Status.LOCKED;
        }
        return Status.ACTIVE;
    }

    /** Share of unfinished progress lost on death, as a whole percentage; 0 when off. */
    public int getDeathPenaltyPercent() {
        double penalty = config.raw().getDouble("quests.death-penalty", 0.0D);
        return (int) Math.round(Math.max(0.0D, Math.min(1.0D, penalty)) * 100);
    }

    /** Current count for one objective. */
    public int getProgress(@Nonnull UUID player, @Nonnull String setId, int objectiveIndex) {
        PlayerQuestData data = players.get(player);
        if (data == null) {
            return 0;
        }
        int[] counts = data.progress.get(setId);
        return counts != null && objectiveIndex < counts.length ? counts[objectiveIndex] : 0;
    }

    /**
     * Pieces of the previous set a player has to wear for this set's quest to count, or 0 when
     * there is no previous set or the requirement is switched off.
     */
    public int getRequiredPieces(@Nonnull ArmorSetDefinition set) {
        String previous = set.getPreviousSetId();
        if (previous == null || !sets.contains(previous)) {
            return 0;
        }
        return Math.max(0, Math.min(4, config.raw().getInt("quests.required-pieces", 4)));
    }

    /** Whether the player is wearing enough of the previous set for this quest to progress. */
    public boolean isWearingRequired(@Nonnull Player player, @Nonnull ArmorSetDefinition set) {
        int required = getRequiredPieces(set);
        if (required == 0) {
            return true;
        }
        ArmorState state = equipmentService.getCachedState(player);
        return state.set() != null
                && state.set().getId().equals(set.getPreviousSetId())
                && state.piecesWorn() >= required;
    }

    /** Plain-text armor requirement, e.g. {@code the full Apprentice set}; null when there is none. */
    @Nullable
    public String describeRequirement(@Nonnull ArmorSetDefinition set) {
        int required = getRequiredPieces(set);
        ArmorSetDefinition previous = required == 0 ? null : sets.get(set.getPreviousSetId());
        if (previous == null) {
            return null;
        }
        return required >= 4
                ? "the full " + previous.getDisplayName() + " set"
                : required + " " + previous.getDisplayName() + " piece" + (required == 1 ? "" : "s");
    }

    /** Every registered set in chain order: entry set first, then each upgrade. */
    @Nonnull
    public List<ArmorSetDefinition> orderedSets() {
        List<ArmorSetDefinition> ordered = new ArrayList<>(sets.values());
        ordered.sort(Comparator.comparingInt(this::depthOf).thenComparing(ArmorSetDefinition::getId));
        return ordered;
    }

    private int depthOf(@Nonnull ArmorSetDefinition set) {
        int depth = 0;
        ArmorSetDefinition current = set;
        while (current != null && current.getPreviousSetId() != null && depth < 16) {
            current = sets.get(current.getPreviousSetId());
            depth++;
        }
        return depth;
    }

    // ---- Progress (main thread) --------------------------------------------------------------

    /** Counts one kill or one mined block towards every quest the player currently has active. */
    public void record(@Nonnull Player player, @Nonnull QuestObjective.Type type, @Nonnull String target) {
        if (!isEnabled() || quests.isEmpty()) {
            return;
        }

        UUID uuid = player.getUniqueId();

        // Collected up front so finishing one quest mid-loop cannot let the same kill count
        // towards the quest it just made available.
        List<ArmorSetDefinition> active = new ArrayList<>();
        for (ArmorQuest quest : quests.values()) {
            ArmorSetDefinition set = sets.get(quest.setId());
            if (set != null && getStatus(uuid, set) == Status.ACTIVE) {
                active.add(set);
            }
        }

        if (active.isEmpty()) {
            return;
        }

        PlayerQuestData data = players.computeIfAbsent(uuid, ignored -> new PlayerQuestData());

        for (ArmorSetDefinition set : active) {
            ArmorQuest quest = quests.get(set.getId());
            int[] counts = data.counts(quest);
            boolean wearing = isWearingRequired(player, set);
            boolean advanced = false;

            for (int index = 0; index < counts.length; index++) {
                QuestObjective objective = quest.objectives().get(index);
                if (counts[index] >= objective.amount()
                        || !objective.matches(type, target)
                        || !objective.meetsConditions(player)) {
                    continue;
                }

                if (!wearing) {
                    messages.actionBar(player, "&cWear " + describeRequirement(set) + " &cto progress the "
                            + legacyColor(set) + set.getDisplayName() + " &cquest");
                    continue;
                }

                counts[index]++;
                advanced = true;
                messages.actionBar(player, legacyColor(set) + set.getDisplayName() + " Quest &8» &f"
                        + objective.describe() + " &8(&a" + counts[index] + "&8/&a" + objective.amount() + "&8)");
            }

            if (!advanced) {
                continue;
            }

            dirty = true;
            if (isFinished(quest, counts)) {
                complete(player, set, data);
            }
        }
    }

    /**
     * Hands in whatever the player is carrying towards the quest's delivery objectives. Items are
     * only taken up to what is still needed, so over-delivering is impossible.
     */
    public void deliver(@Nonnull Player player, @Nonnull ArmorSetDefinition set) {
        ArmorQuest quest = quests.get(set.getId());
        if (quest == null || !quest.hasDeliveries() || getStatus(player.getUniqueId(), set) != Status.ACTIVE) {
            return;
        }

        PlayerQuestData data = players.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerQuestData());
        int[] counts = data.counts(quest);
        ItemStack[] contents = player.getInventory().getStorageContents();
        int delivered = 0;

        for (int index = 0; index < counts.length; index++) {
            QuestObjective objective = quest.objectives().get(index);
            int needed = objective.amount() - counts[index];
            if (objective.type() != QuestObjective.Type.DELIVER || needed <= 0) {
                continue;
            }

            for (int slot = 0; slot < contents.length && needed > 0; slot++) {
                ItemStack stack = contents[slot];
                if (stack == null || stack.getType().isAir()
                        || !objective.matches(QuestObjective.Type.DELIVER, deliveryId(stack))) {
                    continue;
                }

                int taken = Math.min(needed, stack.getAmount());
                if (taken == stack.getAmount()) {
                    contents[slot] = null;
                } else {
                    stack.setAmount(stack.getAmount() - taken);
                }
                needed -= taken;
                counts[index] += taken;
                delivered += taken;
            }
        }

        if (delivered == 0) {
            messages.send(player, "quest-nothing-to-deliver", "&cYou are not carrying anything this quest needs.");
            return;
        }

        player.getInventory().setStorageContents(contents);
        dirty = true;
        messages.send(player, "quest-delivered", "&aDelivered &f{amount} &aitems to the {set} &aquest.",
                Map.of("amount", String.valueOf(delivered), "set", legacyColor(set) + set.getDisplayName()));
        if (config.isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 0.8F);
        }

        if (isFinished(quest, counts)) {
            complete(player, set, data);
        }
    }

    /**
     * Takes a share of unfinished kill, mine and ability progress on death. Deliveries are never
     * lost: those items are already gone.
     */
    public void applyDeathPenalty(@Nonnull Player player) {
        double penalty = Math.max(0.0D, Math.min(1.0D, config.raw().getDouble("quests.death-penalty", 0.0D)));
        PlayerQuestData data = players.get(player.getUniqueId());
        if (!isEnabled() || penalty <= 0.0D || data == null) {
            return;
        }

        for (ArmorQuest quest : quests.values()) {
            ArmorSetDefinition set = sets.get(quest.setId());
            if (set == null || !data.progress.containsKey(quest.setId())
                    || getStatus(player.getUniqueId(), set) != Status.ACTIVE) {
                continue;
            }

            int[] counts = data.counts(quest);
            boolean lost = false;
            for (int index = 0; index < counts.length; index++) {
                QuestObjective objective = quest.objectives().get(index);
                if (objective.type() == QuestObjective.Type.DELIVER
                        || counts[index] <= 0 || counts[index] >= objective.amount()) {
                    continue;
                }
                int kept = (int) Math.floor(counts[index] * (1.0D - penalty));
                lost |= kept != counts[index];
                counts[index] = kept;
            }

            if (lost) {
                dirty = true;
                messages.send(player, "quest-death-penalty",
                        "&cYou died and lost {percent}% of your unfinished {set} &cquest progress.",
                        Map.of("percent", String.valueOf(Math.round(penalty * 100)),
                                "set", legacyColor(set) + set.getDisplayName()));
            }
        }
    }

    private static boolean isFinished(@Nonnull ArmorQuest quest, @Nonnull int[] counts) {
        for (int index = 0; index < counts.length; index++) {
            if (counts[index] < quest.objectives().get(index).amount()) {
                return false;
            }
        }
        return true;
    }

    private void complete(@Nonnull Player player, @Nonnull ArmorSetDefinition set, @Nonnull PlayerQuestData data) {
        data.unlocked.add(set.getId());
        data.progress.remove(set.getId());
        dirty = true;

        String name = legacyColor(set) + set.getDisplayName();
        messages.send(player, "quest-complete",
                "&aQuest complete! You can now forge {set} &aarmor at the Arcane Forge.",
                Map.of("set", name));
        player.showTitle(Title.title(
                messages.legacy(name + " Armor"),
                messages.legacy("&aunlocked at the Arcane Forge")));
        if (config.isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
        }
        grantRewards(player, quests.get(set.getId()));

        for (ArmorSetDefinition next : sets.values()) {
            if (set.getId().equals(next.getPreviousSetId()) && !isUnlocked(player.getUniqueId(), next.getId())) {
                messages.send(player, "quest-available",
                        "&7New armor quest: {set}&7. See &d/arcane quests&7.",
                        Map.of("set", legacyColor(next) + next.getDisplayName()));
            }
        }
    }

    private void grantRewards(@Nonnull Player player, @Nullable ArmorQuest quest) {
        if (quest == null) {
            return;
        }
        if (quest.rewardLevels() > 0) {
            player.giveExpLevels(quest.rewardLevels());
        }
        for (String command : quest.rewardCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", player.getName()));
        }
    }

    // ---- Admin -------------------------------------------------------------------------------

    public void forceComplete(@Nonnull UUID player, @Nonnull String setId) {
        PlayerQuestData data = players.computeIfAbsent(player, ignored -> new PlayerQuestData());
        data.unlocked.add(setId);
        data.progress.remove(setId);
        dirty = true;
    }

    /** Clears one set's quest, or every quest when {@code setId} is null. */
    public void reset(@Nonnull UUID player, @Nullable String setId) {
        PlayerQuestData data = players.get(player);
        if (data == null) {
            return;
        }
        if (setId == null) {
            players.remove(player);
        } else {
            data.unlocked.remove(setId);
            data.progress.remove(setId);
            data.crafted.remove(setId);
        }
        dirty = true;
    }

    // ---- Persistence -------------------------------------------------------------------------

    public void load() {
        players.clear();
        firstForged.clear();
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);

        ConfigurationSection firstsSec = yaml.getConfigurationSection("world-firsts");
        if (firstsSec != null) {
            for (String setId : firstsSec.getKeys(false)) {
                String name = firstsSec.getString(setId);
                if (name != null && !name.isBlank()) {
                    firstForged.put(setId, name);
                }
            }
        }

        for (String key : yaml.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                continue;
            }

            PlayerQuestData data = new PlayerQuestData();
            data.unlocked.addAll(yaml.getStringList(key + ".unlocked"));

            ConfigurationSection progress = yaml.getConfigurationSection(key + ".progress");
            if (progress != null) {
                for (String setId : progress.getKeys(false)) {
                    data.progress.put(setId, progress.getIntegerList(setId).stream()
                            .mapToInt(Integer::intValue).toArray());
                }
            }

            ConfigurationSection craftedSec = yaml.getConfigurationSection(key + ".crafted");
            if (craftedSec != null) {
                for (String setId : craftedSec.getKeys(false)) {
                    data.crafted.computeIfAbsent(setId, k -> ConcurrentHashMap.newKeySet())
                            .addAll(craftedSec.getStringList(setId));
                }
            }

            players.put(uuid, data);
        }
    }

    /** Snapshots on the calling (main) thread, then writes the file off-thread. */
    public void saveAsync() {
        if (!dirty) {
            return;
        }
        dirty = false;
        String contents = snapshot().saveToString();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> write(contents));
    }

    /** Blocking save, for shutdown when the scheduler no longer accepts tasks. */
    public void saveNow() {
        dirty = false;
        write(snapshot().saveToString());
    }

    @Nonnull
    private YamlConfiguration snapshot() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerQuestData> entry : players.entrySet()) {
            String key = entry.getKey().toString();
            PlayerQuestData data = entry.getValue();
            yaml.set(key + ".unlocked", new ArrayList<>(data.unlocked));
            for (Map.Entry<String, int[]> progress : data.progress.entrySet()) {
                yaml.set(key + ".progress." + progress.getKey(),
                        Arrays.stream(progress.getValue()).boxed().toList());
            }
            for (Map.Entry<String, Set<String>> entryCrafted : data.crafted.entrySet()) {
                yaml.set(key + ".crafted." + entryCrafted.getKey(), new ArrayList<>(entryCrafted.getValue()));
            }
        }

        for (Map.Entry<String, String> entry : firstForged.entrySet()) {
            yaml.set("world-firsts." + entry.getKey(), entry.getValue());
        }
        return yaml;
    }

    private void write(@Nonnull String contents) {
        synchronized (writeLock) {
            try {
                Files.createDirectories(dataFile.getParentFile().toPath());
                Files.writeString(dataFile.toPath(), contents, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "Could not save " + DATA_FILE, ex);
            }
        }
    }

    // ---- Helpers -----------------------------------------------------------------------------

    /** The set's colour as an ampersand code, for strings that go through {@link Messages}. */
    @Nonnull
    public static String legacyColor(@Nonnull ArmorSetDefinition set) {
        ChatColor color = set.getColor();
        return "&" + color.getChar();
    }

    /** {@code DEEPSLATE_LAPIS_ORE} to {@code Deepslate Lapis Ore}. */
    @Nonnull
    private static String prettify(@Nonnull String enumName) {
        StringBuilder builder = new StringBuilder();
        for (String word : enumName.toLowerCase(Locale.ROOT).split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }

    /** One player's unlocked sets and in-flight counters. */
    private static final class PlayerQuestData {

        private final Set<String> unlocked = ConcurrentHashMap.newKeySet();
        private final Map<String, int[]> progress = new ConcurrentHashMap<>();
        private final Map<String, Set<String>> crafted = new ConcurrentHashMap<>();

        /** Counters for a quest, resized if the quest gained or lost objectives since they were saved. */
        @Nonnull
        int[] counts(@Nonnull ArmorQuest quest) {
            int size = quest.objectives().size();
            int[] counts = progress.get(quest.setId());
            if (counts == null || counts.length != size) {
                counts = counts == null ? new int[size] : Arrays.copyOf(counts, size);
                progress.put(quest.setId(), counts);
            }
            return counts;
        }
    }
}
