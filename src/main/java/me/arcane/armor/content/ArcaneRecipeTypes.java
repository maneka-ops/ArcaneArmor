package me.arcane.armor.content;

import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import javax.annotation.Nonnull;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Recipe types, so the Slimefun guide shows which machine makes what.
 *
 * <p>Initialised after {@link ArcaneMachineItems} because each type displays the machine block it
 * belongs to.
 */
public final class ArcaneRecipeTypes {

    private static RecipeType forge;
    private static RecipeType infuser;
    private static RecipeType extractor;
    private static RecipeType compressor;
    private static RecipeType charger;
    private static RecipeType engraver;
    private static RecipeType altar;

    private ArcaneRecipeTypes() {
    }

    public static void init(@Nonnull Plugin plugin) {
        if (forge != null) {
            return;
        }

        forge = new RecipeType(
                new NamespacedKey(plugin, "arcane_forge"), ArcaneMachineItems.ARCANE_FORGE);
        infuser = new RecipeType(
                new NamespacedKey(plugin, "arcane_infuser"), ArcaneMachineItems.ARCANE_INFUSER);
        extractor = new RecipeType(
                new NamespacedKey(plugin, "essence_extractor"), ArcaneMachineItems.ESSENCE_EXTRACTOR);
        compressor = new RecipeType(
                new NamespacedKey(plugin, "arcane_compressor"), ArcaneMachineItems.ARCANE_COMPRESSOR);
        charger = new RecipeType(
                new NamespacedKey(plugin, "crystal_charger"), ArcaneMachineItems.CRYSTAL_CHARGER);
        engraver = new RecipeType(
                new NamespacedKey(plugin, "rune_engraver"), ArcaneMachineItems.RUNE_ENGRAVER);
        altar = new RecipeType(
                new NamespacedKey(plugin, "upgrade_altar"), ArcaneMachineItems.UPGRADE_ALTAR);
    }

    @Nonnull
    public static RecipeType forge() {
        return require(forge);
    }

    @Nonnull
    public static RecipeType infuser() {
        return require(infuser);
    }

    @Nonnull
    public static RecipeType extractor() {
        return require(extractor);
    }

    @Nonnull
    public static RecipeType compressor() {
        return require(compressor);
    }

    @Nonnull
    public static RecipeType charger() {
        return require(charger);
    }

    @Nonnull
    public static RecipeType engraver() {
        return require(engraver);
    }

    @Nonnull
    public static RecipeType altar() {
        return require(altar);
    }

    @Nonnull
    private static RecipeType require(RecipeType type) {
        if (type == null) {
            throw new IllegalStateException("ArcaneRecipeTypes.init(plugin) has not run yet");
        }
        return type;
    }
}
