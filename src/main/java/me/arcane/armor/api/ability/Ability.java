package me.arcane.armor.api.ability;

import javax.annotation.Nonnull;
import me.arcane.armor.api.registry.Keyed;

/**
 * An active or ultimate ability granted by an armor set.
 *
 * <p>Implementations are stateless singletons shared by every player using them; per-player state
 * belongs in the services (cooldowns, energy) or in a map keyed by UUID inside the implementation.
 * {@link #execute(AbilityContext)} always runs on the main thread.
 *
 * <p>Energy and cooldown values here are defaults: the ability service prefers the values in
 * {@code config.yml} when present, so servers can retune everything without a rebuild.
 */
public interface Ability extends Keyed {

    /** Name shown in GUIs, the action bar and {@code %arcane_cooldown%}. */
    @Nonnull
    String getDisplayName();

    @Nonnull
    AbilityTrigger getTrigger();

    /** Energy drained on a successful cast, before module discounts. */
    int getDefaultEnergyCost();

    /** Cooldown in seconds at {@link me.arcane.armor.api.armor.ArmorTier#TIER_1}. */
    double getDefaultCooldownSeconds();

    /**
     * Runs the ability. Called on the main thread only after energy, cooldown, permission and
     * piece-count checks have all passed.
     */
    void execute(@Nonnull AbilityContext context);

    /**
     * Extra gate evaluated before energy is taken, for abilities that need a valid target or a
     * particular situation. Returning {@code false} aborts the cast without spending anything.
     */
    default boolean canUse(@Nonnull AbilityContext context) {
        return true;
    }

    /** One-line description for the armor info GUI. */
    @Nonnull
    default String getDescription() {
        return "";
    }
}
