package me.arcane.armor.integration;

import javax.annotation.Nonnull;
import me.arcane.armor.api.ability.Ability;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.api.module.ArmorModule;
import me.arcane.armor.core.armor.ArmorState;
import me.arcane.armor.core.armor.EquipmentService;
import me.arcane.armor.core.cooldown.CooldownService;
import me.arcane.armor.registry.ArcaneRegistries;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * PlaceholderAPI bridge.
 *
 * <p>Every placeholder reads the cached armor state, so a scoreboard refreshing twenty times a
 * second costs a map lookup rather than an inventory scan. Offline players resolve to empty values
 * instead of loading anything from disk.
 *
 * <p>Registered only when PlaceholderAPI is present; the class is never touched otherwise, so its
 * absence cannot produce a {@code NoClassDefFoundError}.
 */
public final class PlaceholderHook extends PlaceholderExpansion {

    private final Plugin plugin;
    private final EquipmentService equipmentService;
    private final CooldownService cooldownService;
    private final ArcaneRegistries registries;

    public PlaceholderHook(
            @Nonnull Plugin plugin,
            @Nonnull EquipmentService equipmentService,
            @Nonnull CooldownService cooldownService,
            @Nonnull ArcaneRegistries registries) {
        this.plugin = plugin;
        this.equipmentService = equipmentService;
        this.cooldownService = cooldownService;
        this.registries = registries;
    }

    @Nonnull
    @Override
    public String getIdentifier() {
        return "arcane";
    }

    @Nonnull
    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Nonnull
    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        // Survive a PlaceholderAPI reload; we are cheap to keep resident.
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @Nonnull String params) {
        if (!(offlinePlayer instanceof Player player)) {
            return "";
        }

        ArmorState state = equipmentService.getCachedState(player);
        ArmorSetDefinition set = state.set();

        if (params.startsWith("cooldown_")) {
            String abilityId = params.substring("cooldown_".length());
            Ability ability = registries.abilities().get(abilityId);
            return ability == null
                    ? ""
                    : String.valueOf(cooldownService.getRemainingSeconds(player.getUniqueId(), ability.getId()));
        }

        return switch (params) {
            case "energy" -> String.valueOf(state.energy());
            case "energy_max" -> String.valueOf(state.capacity());
            case "energy_percent" -> String.valueOf(Math.round(state.energyFraction() * 100.0D));
            case "set" -> set == null ? "" : set.getDisplayName();
            case "set_id" -> set == null ? "" : set.getId();
            case "level", "tier" -> state.isWearingAny() ? state.tier().getLabel() : "";
            case "upgrade" -> upgradeLabel(state);
            case "pieces" -> String.valueOf(state.piecesWorn());
            case "modules" -> String.valueOf(state.modules().size());
            case "modules_list" -> joinModules(state);
            case "runes" -> String.valueOf(state.runes().size());
            case "full_set" -> String.valueOf(state.isFullSet());
            default -> null;
        };
    }

    @Nonnull
    private static String upgradeLabel(@Nonnull ArmorState state) {
        if (!state.isWearingAny()) {
            return "";
        }
        return state.tier().next() == null ? "MAX" : state.tier().next().getLabel();
    }

    @Nonnull
    private static String joinModules(@Nonnull ArmorState state) {
        StringBuilder builder = new StringBuilder();
        for (ArmorModule module : state.modules()) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(module.getDisplayName());
        }
        return builder.toString();
    }
}
