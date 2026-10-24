package me.arcane.armor.core.data;

import java.util.List;
import javax.annotation.Nonnull;
import me.arcane.armor.api.armor.ArmorTier;

/**
 * Immutable snapshot of the Arcane state stored on one armor item.
 *
 * <p>Everything here lives in the item's persistent data container, so it survives drops, chests,
 * server restarts and /reload without any plugin-side storage to keep in sync.
 *
 * @param setId id of the owning {@link me.arcane.armor.api.armor.ArmorSetDefinition}
 * @param tier current upgrade tier
 * @param energy stored Arcane Energy
 * @param moduleIds installed module ids, in slot order
 * @param runeIds engraved rune ids, in engraving order
 */
public record ArmorData(
        @Nonnull String setId,
        @Nonnull ArmorTier tier,
        int energy,
        @Nonnull List<String> moduleIds,
        @Nonnull List<String> runeIds) {

    public ArmorData {
        moduleIds = List.copyOf(moduleIds);
        runeIds = List.copyOf(runeIds);
        energy = Math.max(0, energy);
    }

    @Nonnull
    public ArmorData withEnergy(int newEnergy) {
        return new ArmorData(setId, tier, newEnergy, moduleIds, runeIds);
    }

    @Nonnull
    public ArmorData withTier(@Nonnull ArmorTier newTier) {
        return new ArmorData(setId, newTier, energy, moduleIds, runeIds);
    }

    @Nonnull
    public ArmorData withModules(@Nonnull List<String> newModules) {
        return new ArmorData(setId, tier, energy, newModules, runeIds);
    }

    @Nonnull
    public ArmorData withRunes(@Nonnull List<String> newRunes) {
        return new ArmorData(setId, tier, energy, moduleIds, newRunes);
    }

    public boolean hasFreeModuleSlot() {
        return moduleIds.size() < tier.getModuleSlots();
    }

    public boolean hasFreeRuneSlot() {
        return runeIds.size() < tier.getRuneSlots();
    }
}
