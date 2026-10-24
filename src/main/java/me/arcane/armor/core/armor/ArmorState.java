package me.arcane.armor.core.armor;

import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.api.armor.ArmorTier;
import me.arcane.armor.api.armor.SetTrait;
import me.arcane.armor.api.module.ArmorModule;
import me.arcane.armor.api.rune.Rune;

/**
 * What a player is wearing right now, folded into one immutable value.
 *
 * <p>Computed once per tick pass by {@link EquipmentService} and then read by every listener, so
 * a melee hit or a fall never re-reads four persistent data containers on the hot path.
 *
 * @param set the dominant set, or {@code null} when the player wears no Arcane armor
 * @param piecesWorn how many pieces of {@code set} are worn (0-4)
 * @param tier lowest tier among the worn pieces, so a single lagging piece holds the set back
 * @param modules every module installed across the worn pieces
 * @param runes every rune engraved across the worn pieces
 * @param traits union of set, module and rune traits
 * @param energy total stored energy across the worn pieces
 * @param capacity total energy capacity across the worn pieces
 * @param energyDiscount combined ability discount, 0.0 - 0.9
 * @param damageBonus combined outgoing melee multiplier
 * @param damageReduction combined incoming reduction, capped at 0.8
 */
public record ArmorState(
        @Nullable ArmorSetDefinition set,
        int piecesWorn,
        @Nonnull ArmorTier tier,
        @Nonnull List<ArmorModule> modules,
        @Nonnull List<Rune> runes,
        @Nonnull Set<SetTrait> traits,
        int energy,
        int capacity,
        double energyDiscount,
        double damageBonus,
        double damageReduction) {

    /** Shared instance for players wearing nothing of ours; avoids allocating on every miss. */
    public static final ArmorState EMPTY = new ArmorState(
            null, 0, ArmorTier.TIER_1, List.of(), List.of(), Set.of(), 0, 0, 0.0D, 0.0D, 0.0D);

    public boolean isWearingAny() {
        return set != null && piecesWorn > 0;
    }

    public boolean isFullSet() {
        return piecesWorn >= 4;
    }

    public boolean hasTrait(@Nonnull SetTrait trait) {
        return traits.contains(trait);
    }

    /** Energy as a 0.0 - 1.0 fraction, for bars and placeholders. */
    public double energyFraction() {
        return capacity <= 0 ? 0.0D : Math.min(1.0D, (double) energy / capacity);
    }
}
