package me.arcane.armor.api;

import javax.annotation.Nonnull;
import me.arcane.armor.api.ability.Ability;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.api.module.ArmorModule;
import me.arcane.armor.api.registry.Registry;
import me.arcane.armor.api.rune.Rune;
import me.arcane.armor.core.ability.AbilityService;
import me.arcane.armor.core.armor.ArmorState;
import me.arcane.armor.core.cooldown.CooldownService;
import me.arcane.armor.core.data.ArmorDataService;
import me.arcane.armor.core.energy.EnergyService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Public entry point for other plugins.
 *
 * <p>Obtain it from the services manager:
 * <pre>{@code
 * ArcaneArmorAPI api = Bukkit.getServicesManager().load(ArcaneArmorAPI.class);
 * }</pre>
 *
 * <p>To register your own content, do it from your {@code onEnable} with a {@code depend} on
 * ArcaneArmor — the registries freeze once our startup finishes, and anything registered after
 * that throws rather than silently going missing.
 *
 * <pre>{@code
 * api.getAbilities().register(new MyAbility());
 * api.getSets().register(ArmorSetDefinition.builder("mine", "Mine")
 *         .active("my_ability")
 *         .build());
 * }</pre>
 */
public interface ArcaneArmorAPI {

    /** Abilities available to sets. Register before the addon finishes enabling. */
    @Nonnull
    Registry<Ability> getAbilities();

    /** Installable armor modules. */
    @Nonnull
    Registry<ArmorModule> getModules();

    /** Engravable runes. */
    @Nonnull
    Registry<Rune> getRunes();

    /** Armor set definitions. */
    @Nonnull
    Registry<ArmorSetDefinition> getSets();

    /**
     * What a player is wearing right now.
     *
     * <p>Reads a cache refreshed once per tick interval, so it is safe to call from any thread and
     * cheap enough to call per event.
     */
    @Nonnull
    ArmorState getArmorState(@Nonnull Player player);

    /** Casting, with every cooldown, energy and permission check applied. */
    @Nonnull
    AbilityService getAbilityService();

    /** Reading and writing energy on worn armor. Main thread only. */
    @Nonnull
    EnergyService getEnergyService();

    /** Per-player ability cooldowns. */
    @Nonnull
    CooldownService getCooldownService();

    /** Reading and writing the Arcane state stored on an item. */
    @Nonnull
    ArmorDataService getArmorDataService();

    /**
     * Convenience lookup for the registered service.
     *
     * @return the API, or {@code null} if ArcaneArmor is not enabled
     */
    static ArcaneArmorAPI get() {
        return Bukkit.getServicesManager().load(ArcaneArmorAPI.class);
    }
}
