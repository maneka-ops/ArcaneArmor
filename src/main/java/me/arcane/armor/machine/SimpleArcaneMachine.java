package me.arcane.armor.machine;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import javax.annotation.Nonnull;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * A plain recipe machine: items in, items out, energy consumed per tick.
 *
 * <p>The Forge, Infuser, Extractor, Compressor and Charger differ only in their recipe tables and
 * their progress icon, so they share this class instead of each getting a near-empty subclass.
 * Machines that transform an item in place rather than consuming it — the Rune Engraver and the
 * Upgrade Altar — need their own logic and do not use this.
 */
public class SimpleArcaneMachine extends AbstractArcaneMachine {

    private final ItemStack progressBar;

    public SimpleArcaneMachine(
            @Nonnull ItemGroup itemGroup,
            @Nonnull SlimefunItemStack item,
            @Nonnull RecipeType recipeType,
            @Nonnull ItemStack[] recipe,
            @Nonnull Material progressBarMaterial) {
        super(itemGroup, item, recipeType, recipe);
        this.progressBar = new ItemStack(progressBarMaterial);
    }

    @Nonnull
    @Override
    protected ItemStack getProgressBar() {
        return progressBar;
    }
}
