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
import me.arcane.armor.core.armor.ArmorState;
import me.arcane.armor.core.armor.EquipmentService;
import me.arcane.armor.core.data.ArmorData;
import me.arcane.armor.core.data.ArmorDataService;
import me.arcane.armor.core.energy.EnergyService;
import me.arcane.armor.core.quest.QuestService;
import me.arcane.armor.gui.ArmorInfoGui;
import me.arcane.armor.gui.QuestGui;
import me.arcane.armor.registry.ArcaneRegistries;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
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
            List.of("help", "give", "energy", "modules", "quests", "upgrade", "reload", "debug");

    private final ArcaneRegistries registries;
    private final ArmorDataService dataService;
    private final EquipmentService equipmentService;
    private final EnergyService energyService;
    private final ArmorInfoGui armorInfoGui;
    private final QuestService questService;
    private final QuestGui questGui;
    private final ArcaneConfig config;
    private final Messages messages;

    public ArcaneCommand(
            @Nonnull ArcaneRegistries registries,
            @Nonnull ArmorDataService dataService,
            @Nonnull EquipmentService equipmentService,
            @Nonnull EnergyService energyService,
            @Nonnull ArmorInfoGui armorInfoGui,
            @Nonnull QuestService questService,
            @Nonnull QuestGui questGui,
            @Nonnull ArcaneConfig config,
            @Nonnull Messages messages) {
        this.registries = registries;
        this.dataService = dataService;
        this.equipmentService = equipmentService;
        this.energyService = energyService;
        this.armorInfoGui = armorInfoGui;
        this.questService = questService;
        this.questGui = questGui;
        this.config = config;
        this.messages = messages;
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
        if (sender.hasPermission("arcane.admin")) {
            sender.sendMessage(messages.legacy("&d/arcane quests complete <player> <set> &7- unlock a set"));
            sender.sendMessage(messages.legacy("&d/arcane quests reset <player> [set] &7- clear quest progress"));
        }
        sender.sendMessage(messages.legacy("&d/arcane give <player> <set> <slot> [tier] &7- grant a piece"));
        sender.sendMessage(messages.legacy("&d/arcane upgrade &7- raise the held piece one tier"));
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
        dataService.update(stack, data -> data.withTier(tier));

        target.getInventory().addItem(stack);
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

        dataService.write(held, data.withTier(next));
        equipmentService.refresh(player);
        sender.sendMessage(messages.legacy("&aUpgraded to tier &f" + next.getLabel() + "&a."));
        return true;
    }

    private boolean reload(@Nonnull CommandSender sender) {
        if (!sender.hasPermission("arcane.reload")) {
            messages.send(sender, "no-permission", "&cYou lack permission to do that.");
            return true;
        }

        config.reload();
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
