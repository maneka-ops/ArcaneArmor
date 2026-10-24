package me.arcane.armor.api.armor;

import javax.annotation.Nonnull;
import org.bukkit.potion.PotionEffectType;

/**
 * A potion effect a set grants while enough of its pieces are worn.
 *
 * @param type the effect
 * @param amplifier zero-based amplifier
 * @param minPieces how many pieces of the set must be worn before it applies
 */
public record PassiveEffect(@Nonnull PotionEffectType type, int amplifier, int minPieces) {

    public PassiveEffect {
        if (amplifier < 0) {
            throw new IllegalArgumentException("amplifier must not be negative");
        }
        if (minPieces < 1 || minPieces > 4) {
            throw new IllegalArgumentException("minPieces must be between 1 and 4");
        }
    }

    /** Convenience for effects that only come online with the full set. */
    @Nonnull
    public static PassiveEffect fullSet(@Nonnull PotionEffectType type, int amplifier) {
        return new PassiveEffect(type, amplifier, 4);
    }

    @Nonnull
    public static PassiveEffect perPiece(@Nonnull PotionEffectType type, int amplifier) {
        return new PassiveEffect(type, amplifier, 1);
    }
}
