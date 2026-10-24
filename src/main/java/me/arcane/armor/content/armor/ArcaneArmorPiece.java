package me.arcane.armor.content.armor;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import javax.annotation.Nonnull;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.api.armor.ArmorSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Slimefun item for one wearable piece.
 *
 * <p>Behaviour deliberately lives in the services rather than here: the piece is inert data and
 * the tick task, listeners and machines read it. That keeps a piece from having to know whether
 * it is currently worn, dropped or sitting in a machine.
 */
public class ArcaneArmorPiece extends SlimefunItem {

    private final ArmorSetDefinition set;
    private final ArmorSlot slot;

    public ArcaneArmorPiece(
            @Nonnull ItemGroup itemGroup,
            @Nonnull SlimefunItemStack item,
            @Nonnull RecipeType recipeType,
            @Nonnull ItemStack[] recipe,
            @Nonnull ArmorSetDefinition set,
            @Nonnull ArmorSlot slot) {
        super(itemGroup, item, recipeType, recipe);
        this.set = set;
        this.slot = slot;
    }

    @Nonnull
    public ArmorSetDefinition getSet() {
        return set;
    }

    @Nonnull
    public ArmorSlot getSlot() {
        return slot;
    }
}
