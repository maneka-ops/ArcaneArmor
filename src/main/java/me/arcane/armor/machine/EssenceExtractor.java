package me.arcane.armor.machine;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import javax.annotation.Nonnull;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * The Essence Extractor: a plain recipe machine with a third input slot.
 *
 * <p>Every essence takes a raw material, its elemental rune and a Slimefun catalyst, so two slots
 * are not enough. The slots are a constant rather than a constructor argument because Slimefun
 * lays out the menu from inside the base constructor, before any field of this class is set.
 */
public class EssenceExtractor extends SimpleArcaneMachine {

    private static final int[] INPUT_SLOTS = {19, 20, 21};

    public EssenceExtractor(
            @Nonnull ItemGroup itemGroup,
            @Nonnull SlimefunItemStack item,
            @Nonnull RecipeType recipeType,
            @Nonnull ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, Material.BREWING_STAND);
    }

    @Nonnull
    @Override
    protected int[] getInputSlots() {
        return INPUT_SLOTS;
    }
}
