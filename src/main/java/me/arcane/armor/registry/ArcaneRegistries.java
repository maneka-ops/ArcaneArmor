package me.arcane.armor.registry;

import javax.annotation.Nonnull;
import me.arcane.armor.api.ability.Ability;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.api.module.ArmorModule;
import me.arcane.armor.api.registry.Registry;
import me.arcane.armor.api.rune.Rune;
import me.arcane.armor.content.ability.BlinkAbility;
import me.arcane.armor.content.ability.BlizzardAbility;
import me.arcane.armor.content.ability.FlameSpinAbility;
import me.arcane.armor.content.ability.GuardianShieldAbility;
import me.arcane.armor.content.ability.HealingWaveAbility;
import me.arcane.armor.content.ability.IceNovaAbility;
import me.arcane.armor.content.ability.InfernoExplosionAbility;
import me.arcane.armor.content.ability.LightningStrikeAbility;
import me.arcane.armor.content.ability.ManaBurstAbility;
import me.arcane.armor.content.ability.ManaWaveAbility;
import me.arcane.armor.content.ability.MeteorAbility;
import me.arcane.armor.content.ability.ThunderstormAbility;
import me.arcane.armor.content.ability.VoidRiftAbility;
import me.arcane.armor.content.armor.ArcaneSets;
import me.arcane.armor.content.module.ArcaneModules;
import me.arcane.armor.content.rune.ArcaneRunes;

/**
 * Holds the four registries and seeds them with the bundled content.
 *
 * <p>Third-party addons register their own abilities, modules, runes and sets here before the
 * registries freeze — that is, from their own {@code onEnable} while ours is still running, which
 * a {@code depend} on ArcaneArmor guarantees.
 */
public final class ArcaneRegistries {

    private final Registry<Ability> abilities = new Registry<>("ability");
    private final Registry<ArmorModule> modules = new Registry<>("module");
    private final Registry<Rune> runes = new Registry<>("rune");
    private final Registry<ArmorSetDefinition> sets = new Registry<>("armor set");

    /** Registers everything shipped with the addon. Call once, before {@link #freeze()}. */
    public void registerDefaults() {
        abilities.register(new ManaWaveAbility());
        abilities.register(new ManaBurstAbility());
        abilities.register(new LightningStrikeAbility());
        abilities.register(new ThunderstormAbility());
        abilities.register(new FlameSpinAbility());
        abilities.register(new InfernoExplosionAbility());
        abilities.register(new IceNovaAbility());
        abilities.register(new BlizzardAbility());
        abilities.register(new BlinkAbility());
        abilities.register(new VoidRiftAbility());
        abilities.register(new HealingWaveAbility());
        abilities.register(new GuardianShieldAbility());
        abilities.register(new MeteorAbility());

        ArcaneModules.all().forEach(modules::register);
        ArcaneRunes.all().forEach(runes::register);
        ArcaneSets.all().forEach(sets::register);
    }

    /** Seals all four registries once startup is complete. */
    public void freeze() {
        abilities.freeze();
        modules.freeze();
        runes.freeze();
        sets.freeze();
    }

    @Nonnull
    public Registry<Ability> abilities() {
        return abilities;
    }

    @Nonnull
    public Registry<ArmorModule> modules() {
        return modules;
    }

    @Nonnull
    public Registry<Rune> runes() {
        return runes;
    }

    @Nonnull
    public Registry<ArmorSetDefinition> sets() {
        return sets;
    }
}
