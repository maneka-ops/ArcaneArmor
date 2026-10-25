package me.arcane.armor.command;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.arcane.armor.api.ability.Ability;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.api.armor.ArmorSlot;
import me.arcane.armor.api.armor.ArmorTier;
import me.arcane.armor.api.event.ArcaneEnergyChangeEvent;
import me.arcane.armor.api.module.ArmorModule;
import me.arcane.armor.config.ArcaneConfig;
import me.arcane.armor.config.Messages;
import me.arcane.armor.content.armor.ArmorItemFactory;
import me.arcane.armor.core.ability.AbilityService;
import me.arcane.armor.core.armor.ArmorState;
import me.arcane.armor.core.armor.EquipmentService;
import me.arcane.armor.core.cosmetic.FxPreferences;
import me.arcane.armor.core.data.ArmorData;
import me.arcane.armor.core.data.ArmorDataService;
import me.arcane.armor.core.data.AscensionRecords;
import me.arcane.armor.core.energy.EnergyService;
import me.arcane.armor.core.quest.QuestService;
import me.arcane.armor.gui.ArmorInfoGui;
import me.arcane.armor.gui.LeaderboardGui;
import me.arcane.armor.gui.QuestGui;
import me.arcane.armor.machine.ArcaneForge;
import me.arcane.armor.registry.ArcaneRegistries;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * {@code /arcane} and its subcommands.
 *
 * <p>Each subcommand carries its own permission so an operator can hand out {@code arcane.give}
 * without also handing out {@code arcane.reload}. Unknown input falls through to the help text
 * rather than an error, which is what players actually need when they mistype.
 */
public final class ArcaneCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS =
            List.of("help", "give", "energy", "modules", "quests", "upgrade", "forge", "broadcast",
                    "abilities", "top", "fx", "reload", "debug");

    private final ArcaneRegistries registries;
    private final ArmorDataService dataService;
    private final EquipmentService equipmentService;
    private final EnergyService energyService;
    private final ArmorInfoGui armorInfoGui;
    private final QuestService questService;
    private final QuestGui questGui;
    private final ArcaneConfig config;
    private final Messages messages;
    private final ArmorItemFactory itemFactory;
    private final AscensionRecords ascensionRecords;
    private final FxPreferences fxPreferences;
    private final LeaderboardGui leaderboardGui;
    private final AbilityService abilityService;

    public ArcaneCommand(
            @Nonnull ArcaneRegistries registries,
            @Nonnull ArmorDataService dataService,
            @Nonnull EquipmentService equipmentService,
            @Nonnull EnergyService energyService,
            @Nonnull ArmorInfoGui armorInfoGui,
            @Nonnull QuestService questService,
            @Nonnull QuestGui questGui,
            @Nonnull ArcaneConfig config,
            @Nonnull Messages messages,
            @Nonnull ArmorItemFactory itemFactory,
            @Nonnull AscensionRecords ascensionRecords,
            @Nonnull FxPreferences fxPreferences,
            @Nonnull LeaderboardGui leaderboardGui,
            @Nonnull AbilityService abilityService) {
        this.registries = registries;
        this.dataService = dataService;
        this.equipmentService = equipmentService;
        this.energyService = energyService;
        this.armorInfoGui = armorInfoGui;
        this.questService = questService;
        this.questGui = questGui;
        this.config = config;
        this.messages = messages;
        this.itemFactory = itemFactory;
        this.ascensionRecords = ascensionRecords;
        this.fxPreferences = fxPreferences;
        this.leaderboardGui = leaderboardGui;
        this.abilityService = abilityService;
    }

    @Override
    public boolean onCommand(
            @Nonnull CommandSender sender,
            @Nonnull Command command,
            @Nonnull String label,
            @Nonnull String[] args) {

        if (args.length == 0) {
            return help(sender);
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> give(sender, args);
            case "energy" -> energy(sender, args);
            case "modules", "info" -> info(sender);
            case "quests", "quest" -> quests(sender, args);
            case "forge" -> forge(sender, args);
            case "broadcast" -> broadcast(sender, args);
            case "top" -> top(sender);
            case "fx" -> fx(sender);
            case "abilities" -> abilities(sender, args);
            case "upgrade" -> upgrade(sender);
            case "reload" -> reload(sender);
            case "debug" -> debug(sender);
            default -> help(sender);
        };
    }

    private boolean help(@Nonnull CommandSender sender) {
        sender.sendMessage(messages.legacy("&5&lArcane Armor"));
        sender.sendMessage(messages.legacy("&d/arcane modules &7- inspect what you are wearing"));
        sender.sendMessage(messages.legacy("&d/arcane energy [player] &7- show stored energy"));
        sender.sendMessage(messages.legacy("&d/arcane quests &7- armor quests and what they unlock"));
        sender.sendMessage(messages.legacy("&d/arcane top &7- Ascended and quest leaderboard"));
        sender.sendMessage(messages.legacy("&d/arcane fx &7- toggle your own particle effects"));
        if (sender.hasPermission("arcane.admin")) {
            sender.sendMessage(messages.legacy("&d/arcane quests complete <player> <set> &7- unlock a set"));
            sender.sendMessage(messages.legacy("&d/arcane quests reset <player> [set] &7- clear quest progress"));
            sender.sendMessage(messages.legacy("&d/arcane abilities on|off &7- global ability switch"));
        }
        sender.sendMessage(messages.legacy("&d/arcane give <player> <set> <slot> [tier] &7- grant a piece"));
        sender.sendMessage(messages.legacy("&d/arcane upgrade &7- raise the held piece one tier"));
        sender.sendMessage(messages.legacy("&d/arcane forge reset &7- unclaim the Forge you look at"));
        sender.sendMessage(messages.legacy("&d/arcane broadcast reset [set] &7- clear milestone claims"));
        sender.sendMessage(messages.legacy("&d/arcane reload &7- reload the configuration"));
        sender.sendMessage(messages.legacy("&d/arcane debug &7- registry and cache counts"));
        return true;
    }

    private boolean give(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (!sender.hasPermission("arcane.give")) {
            messages.send(sender, "no-permission", "&cYou lack permission to do that.");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(messages.legacy("&cUsage: /arcane give <player> <set> <slot> [tier]"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            messages.send(sender, "player-not-found", "&cNo such player online.");
            return true;
        }

        ArmorSetDefinition set = registries.sets().get(args[2].toLowerCase(Locale.ROOT));
        if (set == null) {
            sender.sendMessage(messages.legacy("&cUnknown set. Known: " + joinSetIds()));
            return true;
        }

        ArmorSlot slot = parseSlot(args[3]);
        if (slot == null) {
            sender.sendMessage(messages.legacy("&cUnknown slot. Use helmet, chestplate, leggings or boots."));
            return true;
        }

        ArmorTier tier = args.length > 4
                ? ArmorTier.fromId(args[4], set.getStartingTier())
                : set.getStartingTier();

        SlimefunItem item = SlimefunItem.getById(set.getItemId(slot));
        if (item == null) {
            sender.sendMessage(messages.legacy("&cThat piece is not registered."));
            return true;
        }

        ItemStack stack = item.getItem().clone();
        ArmorData updated = dataService.update(stack, data -> data.withTier(tier)).orElse(null);
        if (updated != null) {
            // The tier lives in the data and in the presentation; writing only the data left the
            // piece showing (and fighting at) its starting tier, like the old /arcane give.
            present(stack, updated);
        }

        target.getInventory().addItem(stack);
        equipmentService.refresh(target);
        messages.send(sender, "gave-item", "&aGave {player} a {item}.",
                Map.of("player", target.getName(), "item",
                        ChatColor.stripColor(set.getDisplayName() + " " + slot.getDisplayName())));
        return true;
    }

    private boolean energy(@Nonnull CommandSender sender, @Nonnull String[] args) {
        Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1]) : asPlayer(sender);
        if (target == null) {
            messages.send(sender, "player-not-found", "&cNo such player online.");
            return true;
        }

        if (target != sender && !sender.hasPermission("arcane.admin")) {
            messages.send(sender, "no-permission", "&cYou lack permission to do that.");
            return true;
        }

        ArmorState state = equipmentService.refresh(target);
        sender.sendMessage(messages.legacy("&7Arcane Energy: &b" + state.energy()
                + " &8/ &b" + state.capacity()));
        return true;
    }

    private boolean info(@Nonnull CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            sender.sendMessage(messages.legacy("&cThat is a player-only command."));
            return true;
        }

        armorInfoGui.open(player);
        return true;
    }

    /**
     * {@code /arcane quests} opens the quest menu; the {@code complete} and {@code reset} forms let an
     * admin unlock a set or wipe progress, for support and for testing the Forge gate.
     */
    private boolean quests(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (args.length == 1) {
            Player player = asPlayer(sender);
            if (player == null) {
                sender.sendMessage(messages.legacy("&cThat is a player-only command."));
                return true;
            }
            if (!questService.isEnabled()) {
                sender.sendMessage(messages.legacy("&7Armor quests are turned off; every set can be forged."));
                return true;
            }
            questGui.open(player);
            return true;
        }

        if (!sender.hasPermission("arcane.admin")) {
            messages.send(sender, "no-permission", "&cYou lack permission to do that.");
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        boolean complete = action.equals("complete");
        if ((!complete && !action.equals("reset")) || args.length < 3 || (complete && args.length < 4)) {
            sender.sendMessage(messages.legacy("&cUsage: /arcane quests complete <player> <set>"
                    + " &7or &c/arcane quests reset <player> [set]"));
            return true;
        }

        OfflinePlayer target = findPlayer(args[2]);
        if (target == null) {
            messages.send(sender, "player-not-found", "&cNo such player online.");
            return true;
        }

        ArmorSetDefinition set = null;
        if (args.length > 3) {
            set = registries.sets().get(args[3].toLowerCase(Locale.ROOT));
            if (set == null) {
                sender.sendMessage(messages.legacy("&cUnknown set. Known: " + joinSetIds()));
                return true;
            }
        }

        String name = target.getName() == null ? args[2] : target.getName();
        if (complete) {
            questService.forceComplete(target.getUniqueId(), set.getId());
            sender.sendMessage(messages.legacy("&aUnlocked the " + set.getDisplayName() + " quest for " + name + "."));
        } else {
            questService.reset(target.getUniqueId(), set == null ? null : set.getId());
            sender.sendMessage(messages.legacy("&eReset " + (set == null ? "every quest" : "the "
                    + set.getDisplayName() + " quest") + " for " + name + "."));
        }
        return true;
    }

    /** Online players first, then anyone the server has seen before; never a blocking web lookup. */
    @Nullable
    private static OfflinePlayer findPlayer(@Nonnull String name) {
        Player online = Bukkit.getPlayerExact(name);
        return online != null ? online : Bukkit.getOfflinePlayerIfCached(name);
    }

    /**
     * {@code /arcane forge reset} clears the first-opener claim on the Forge the sender is looking
     * at, so the next player to open it claims it instead.
     *
     * <p>The claim belongs to whoever opened the Forge first and it survives forever after; when
     * that player logs off and the block sits where a friend cannot look into it, or the server
     * simply changes hands, the Forge would stay bound. The owner and any admin both get to cut
     * the tie; resetting a Forge minted for someone else's benefit is always the current claim
     * holder's move, so other players cannot be robbed of their claim by strangers.
     */
    private boolean forge(@Nonnull CommandSender sender, @Nonnull String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            sender.sendMessage(messages.legacy("&cThat is a player-only command."));
            return true;
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("reset")) {
            sender.sendMessage(messages.legacy("&cUsage: /arcane forge reset"));
            return true;
        }

        Block block = player.getTargetBlockExact(5);
        String blockId = block == null ? null : BlockStorage.checkID(block);
        if (blockId == null || !blockId.equals("ARCANE_FORGE")) {
            sender.sendMessage(messages.legacy("&cLook at an Arcane Forge within 5 blocks."));
            return true;
        }

        String owner = ArcaneForge.storedOwner(block);
        boolean allowed = sender.hasPermission("arcane.admin")
                || (owner != null && owner.equals(player.getUniqueId().toString()));
        if (!allowed) {
            messages.send(sender, "no-permission", "&cYou lack permission to do that.");
            return true;
        }

        ArcaneForge.resetOwnership(block);
        sender.sendMessage(messages.legacy("&eForge claim cleared. The next player to open it claims it."));
        return true;
    }

    /**
     * {@code /arcane broadcast reset [set]} clears the one-time milestone claims.
     *
     * <p>World Firsts and First-to-Ascend persist forever by design; after a support or testing
     * session has inked one in, this is the only way to let the milestone fire again. Cleared per
     * set, or across every set when no argument is given. Admin-forged pieces never claim these in
     * the first place, so the reset only has to clean up old data.
     */
    private boolean broadcast(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (!sender.hasPermission("arcane.admin")) {
            messages.send(sender, "no-permission", "&cYou lack permission to do that.");
            return true;
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("reset")) {
            sender.sendMessage(messages.legacy("&cUsage: /arcane broadcast reset [set]"));
            return true;
        }

        String setId = null;
        if (args.length > 2) {
            ArmorSetDefinition set = registries.sets().get(args[2].toLowerCase(Locale.ROOT));
            if (set == null) {
                sender.sendMessage(messages.legacy("&cUnknown set. Known: " + joinSetIds()));
                return true;
            }
            setId = set.getId();
        }

        questService.resetWorldFirsts(setId);
        ascensionRecords.reset(setId);
        sender.sendMessage(messages.legacy("&eBroadcast claims cleared for "
                + (setId == null ? "every set" : setId) + ". The next milestone can be won again."));
        return true;
    }

    /**
     * {@code /arcane top} opens the leaderboard, or prints the top ranks when the sender cannot
     * open menus. Ranking is Ascended pieces first, finished quests second.
     */
    private boolean top(@Nonnull CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            List<LeaderboardGui.Entry> ranked = leaderboardGui.ranking();
            sender.sendMessage(messages.legacy("&5&lArcane Leaderboard"));
            int shown = Math.min(10, ranked.size());
            for (int index = 0; index < shown; index++) {
                LeaderboardGui.Entry entry = ranked.get(index);
                sender.sendMessage(messages.legacy("&e#" + (index + 1) + " &f" + entry.displayName()
                        + " &7- &d" + entry.ascended() + " Ascended &7, &b" + entry.quests() + " quests"));
            }
            return true;
        }

        leaderboardGui.open(player);
        return true;
    }

    /**
     * {@code /arcane fx} mutes or unmutes the effects the caster's own pieces emit.
     *
     * <p>Only personal decoration is muted — the aura ring, forge and upgrade celebrations and
     * ability bursts. Server-wide milestone toasts are announcements and still reach everyone.
     */
    private boolean fx(@Nonnull CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            sender.sendMessage(messages.legacy("&cThat is a player-only command."));
            return true;
        }

        boolean muted = fxPreferences.toggle(player.getUniqueId());
        if (muted) {
            sender.sendMessage(messages.legacy("&ePersonal particle effects muted. &7Run again to unmute."));
        } else {
            sender.sendMessage(messages.legacy("&aPersonal particle effects enabled."));
        }
        return true;
    }

    /**
     * {@code /arcane abilities on|off} flips the global ability switch.
     *
     * <p>A blunt instrument by design: every set, every player, every trigger stops at once —
     * the honest tool for a broken event, a moderation night or a tournament. The toggle lives
     * only in memory, so a reload or a restart restores the config's own value and it cannot
     * end up forgotten for weeks.
     */
    private boolean abilities(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (!sender.hasPermission("arcane.admin")) {
            messages.send(sender, "no-permission", "&cYou lack permission to do that.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(messages.legacy("&7Arcane abilities are currently &f"
                    + (abilityService.isAllEnabled() ? "enabled" : "disabled") + "&7."));
            sender.sendMessage(messages.legacy("&cUsage: /arcane abilities on|off"));
            return true;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "on", "enable", "enabled" -> {
                abilityService.setAllEnabled(true);
                sender.sendMessage(messages.legacy("&aArcane abilities enabled for every player."));
            }
            case "off", "disable", "disabled" -> {
                abilityService.setAllEnabled(false);
                sender.sendMessage(messages.legacy("&cArcane abilities disabled for every player."));
            }
            default -> sender.sendMessage(messages.legacy("&cUsage: /arcane abilities on|off"));
        }
        return true;
    }

    /** Admin shortcut past the Upgrade Altar, for testing and for support. */
    private boolean upgrade(@Nonnull CommandSender sender) {
        if (!sender.hasPermission("arcane.admin")) {
            messages.send(sender, "no-permission", "&cYou lack permission to do that.");
            return true;
        }

        Player player = asPlayer(sender);
        if (player == null) {
            sender.sendMessage(messages.legacy("&cThat is a player-only command."));
            return true;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        ArmorData data = dataService.read(held).orElse(null);
        if (data == null) {
            sender.sendMessage(messages.legacy("&cHold a piece of Arcane armor."));
            return true;
        }

        ArmorTier next = data.tier().next();
        if (next == null) {
            sender.sendMessage(messages.legacy("&cThat piece is already Ascended."));
            return true;
        }

        ArmorData upgraded = data.withTier(next);
        dataService.write(held, upgraded);
        present(held, upgraded);
        equipmentService.refresh(player);
        sender.sendMessage(messages.legacy("&aUpgraded to tier &f" + next.getLabel() + "&a."));
        return true;
    }

    /**
     * Rebuilds a piece's name, lore, attributes and enchantments after its data changed.
     *
     * <p>A tier change is only real when the piece both carries and shows it; the Upgrade Altar
     * calls the same factory routine, and the {@code /arcane give} and {@code /arcane upgrade}
     * shortcuts now go through here too.
     */
    private void present(@Nonnull ItemStack item, @Nonnull ArmorData data) {
        ArmorSetDefinition set = registries.sets().get(data.setId());
        ArmorSlot slot = slotOf(item);
        if (set != null && slot != null) {
            itemFactory.refreshPresentation(item, set, slot, data, config.getDefenseMultiplier());
        }
    }

    /** Works the slot out from the item's material, which is enough for both armor bases. */
    @Nullable
    private static ArmorSlot slotOf(@Nonnull ItemStack item) {
        String type = item.getType().name();
        if (type.endsWith("_HELMET")) {
            return ArmorSlot.HELMET;
        }
        if (type.endsWith("_CHESTPLATE")) {
            return ArmorSlot.CHESTPLATE;
        }
        if (type.endsWith("_LEGGINGS")) {
            return ArmorSlot.LEGGINGS;
        }
        if (type.endsWith("_BOOTS")) {
            return ArmorSlot.BOOTS;
        }
        return null;
    }

    private boolean reload(@Nonnull CommandSender sender) {
        if (!sender.hasPermission("arcane.reload")) {
            messages.send(sender, "no-permission", "&cYou lack permission to do that.");
            return true;
        }

        config.reload();
        abilityService.setAllEnabled(config.isAllAbilitiesEnabled());
        questService.reloadDefinitions();
        equipmentService.clear();
        for (Player online : Bukkit.getOnlinePlayers()) {
            equipmentService.refresh(online);
        }

        messages.send(sender, "reloaded", "&aConfiguration reloaded.");
        return true;
    }

    private boolean debug(@Nonnull CommandSender sender) {
        if (!sender.hasPermission("arcane.debug")) {
            messages.send(sender, "no-permission", "&cYou lack permission to do that.");
            return true;
        }

        sender.sendMessage(messages.legacy("&7Sets: &f" + registries.sets().size()
                + " &7Abilities: &f" + registries.abilities().size()
                + " &7Modules: &f" + registries.modules().size()
                + " &7Runes: &f" + registries.runes().size()));

        Player player = asPlayer(sender);
        if (player != null) {
            ArmorState state = equipmentService.getCachedState(player);
            sender.sendMessage(messages.legacy("&7Worn: &f"
                    + (state.set() == null ? "none" : state.set().getId())
                    + " &7pieces: &f" + state.piecesWorn()
                    + " &7tier: &f" + state.tier().getLabel()
                    + " &7traits: &f" + state.traits().size()));
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(
            @Nonnull CommandSender sender,
            @Nonnull Command command,
            @Nonnull String alias,
            @Nonnull String[] args) {

        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }

        if (args[0].equalsIgnoreCase("give")) {
            return switch (args.length) {
                case 2 -> null; // Bukkit fills in player names
                case 3 -> filter(registries.sets().values().stream()
                        .map(ArmorSetDefinition::getId).toList(), args[2]);
                case 4 -> filter(Arrays.stream(ArmorSlot.values())
                        .map(slot -> slot.name().toLowerCase(Locale.ROOT)).toList(), args[3]);
                case 5 -> filter(Arrays.stream(ArmorTier.values())
                        .map(tier -> tier.name().toLowerCase(Locale.ROOT)).toList(), args[4]);
                default -> List.of();
            };
        }

        if (args[0].equalsIgnoreCase("quests") || args[0].equalsIgnoreCase("quest")) {
            if (!sender.hasPermission("arcane.admin")) {
                return List.of();
            }
            return switch (args.length) {
                case 2 -> filter(List.of("complete", "reset"), args[1]);
                case 3 -> null;
                case 4 -> filter(registries.sets().values().stream()
                        .map(ArmorSetDefinition::getId).toList(), args[3]);
                default -> List.of();
            };
        }

        if (args[0].equalsIgnoreCase("forge")) {
            return args.length == 2 ? filter(List.of("reset"), args[1]) : List.of();
        }

        if (args[0].equalsIgnoreCase("broadcast")) {
            return switch (args.length) {
                case 2 -> filter(List.of("reset"), args[1]);
                case 3 -> filter(registries.sets().values().stream()
                        .map(ArmorSetDefinition::getId).toList(), args[2]);
                default -> List.of();
            };
        }

        if (args[0].equalsIgnoreCase("abilities")) {
            if (!sender.hasPermission("arcane.admin")) {
                return List.of();
            }
            return args.length == 2 ? filter(List.of("on", "off"), args[1]) : List.of();
        }

        return List.of();
    }

    @Nonnull
    private static List<String> filter(@Nonnull List<String> options, @Nonnull String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(lower)) {
                matches.add(option);
            }
        }
        return matches;
    }

    @Nullable
    private static ArmorSlot parseSlot(@Nonnull String raw) {
        for (ArmorSlot slot : ArmorSlot.values()) {
            if (slot.name().equalsIgnoreCase(raw)) {
                return slot;
            }
        }
        return null;
    }

    @Nonnull
    private String joinSetIds() {
        StringBuilder builder = new StringBuilder();
        for (ArmorSetDefinition set : registries.sets().values()) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(set.getId());
        }
        return builder.toString();
    }

    @Nullable
    private static Player asPlayer(@Nonnull CommandSender sender) {
        return sender instanceof Player player ? player : null;
    }

    /** Kept for the API: charges a player's set from another plugin or a command block. */
    public void charge(@Nonnull Player player, int amount) {
        energyService.charge(player, amount, ArcaneEnergyChangeEvent.Cause.EXTERNAL);
    }

    /** Exposed for tab completion in sibling commands. */
    @Nonnull
    public List<String> moduleIds() {
        return registries.modules().values().stream().map(ArmorModule::getId).toList();
    }

    /** Exposed for tab completion in sibling commands. */
    @Nonnull
    public List<String> abilityIds() {
        return registries.abilities().values().stream().map(Ability::getId).toList();
    }
}
