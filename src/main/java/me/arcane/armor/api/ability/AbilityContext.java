package me.arcane.armor.api.ability;

import javax.annotation.Nonnull;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.api.armor.ArmorTier;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Everything an ability is allowed to know about the cast that invoked it.
 *
 * <p>Instances are created on the main thread by the ability service and are never retained, so an
 * ability that wants to do work later must schedule it against {@link #plugin()} rather than
 * holding on to the context.
 *
 * @param player the caster
 * @param set the set that granted the ability
 * @param tier the lowest tier among the worn pieces, which is what the cast scales from
 * @param piecesWorn how many pieces of {@code set} the caster wears (1-4)
 * @param energySpent energy actually deducted for this cast
 * @param plugin owning plugin, for scheduling
 */
public record AbilityContext(
        @Nonnull Player player,
        @Nonnull ArmorSetDefinition set,
        @Nonnull ArmorTier tier,
        int piecesWorn,
        int energySpent,
        @Nonnull Plugin plugin) {

    @Nonnull
    public Location location() {
        return player.getLocation();
    }

    @Nonnull
    public World world() {
        return player.getWorld();
    }

    /** Scales a base value by the caster's tier, the standard way abilities grow. */
    public double scaled(double base) {
        return base * set.effectiveTier(tier).getStatMultiplier();
    }

    /** Scales and rounds, for values that must stay integral (damage ticks, block radii). */
    public int scaledInt(double base) {
        return (int) Math.round(scaled(base));
    }
}
