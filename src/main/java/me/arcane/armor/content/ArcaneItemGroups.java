package me.arcane.armor.content;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.NestedItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.SubItemGroup;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import javax.annotation.Nonnull;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Guide layout: one nested group with a page per system, so the guide reads as a progression
 * rather than as one flat wall of items.
 *
 * <p>Built lazily from {@link #init(Plugin)} because the groups need a {@link NamespacedKey}, and
 * therefore a plugin instance, which only exists once the server enables us.
 */
public final class ArcaneItemGroups {

    private static NestedItemGroup main;
    private static ItemGroup gear;
    private static ItemGroup materials;
    private static ItemGroup machines;
    private static ItemGroup armor;
    private static ItemGroup modules;
    private static ItemGroup runes;

    private ArcaneItemGroups() {
    }

    public static void init(@Nonnull Plugin plugin) {
        if (main != null) {
            return;
        }

        main = new NestedItemGroup(
                new NamespacedKey(plugin, "arcane_main"),
                CustomItemStack.create(Material.NETHERITE_CHESTPLATE, "&5&lArcane Armor"));

        materials = new SubItemGroup(
                new NamespacedKey(plugin, "arcane_materials"), main,
                CustomItemStack.create(Material.NETHERITE_SCRAP, "&d&lArcane Materials"));

        machines = new SubItemGroup(
                new NamespacedKey(plugin, "arcane_machines"), main,
                CustomItemStack.create(Material.ENCHANTING_TABLE, "&b&lArcane Machines"));

        armor = new SubItemGroup(
                new NamespacedKey(plugin, "arcane_armor"), main,
                CustomItemStack.create(Material.NETHERITE_CHESTPLATE, "&5&lArcane Armor Sets"));

        gear = new SubItemGroup(
                new NamespacedKey(plugin, "arcane_gear"), main,
                CustomItemStack.create(Material.NETHERITE_SWORD, "&c&lArcane Gear"));

        modules = new SubItemGroup(
                new NamespacedKey(plugin, "arcane_modules"), main,
                CustomItemStack.create(Material.REDSTONE_TORCH, "&a&lArmor Modules"));

        runes = new SubItemGroup(
                new NamespacedKey(plugin, "arcane_runes"), main,
                CustomItemStack.create(Material.ECHO_SHARD, "&3&lRunes"));
    }

    @Nonnull
    public static NestedItemGroup main() {
        return require(main);
    }

    @Nonnull
    public static ItemGroup materials() {
        return require(materials);
    }

    @Nonnull
    public static ItemGroup machines() {
        return require(machines);
    }

    /** Weapons, tools, shields and bows. */
    @Nonnull
    public static ItemGroup gear() {
        return require(gear);
    }

    @Nonnull
    public static ItemGroup armor() {
        return require(armor);
    }

    @Nonnull
    public static ItemGroup modules() {
        return require(modules);
    }

    @Nonnull
    public static ItemGroup runes() {
        return require(runes);
    }

    @Nonnull
    private static <T> T require(T group) {
        if (group == null) {
            throw new IllegalStateException("ArcaneItemGroups.init(plugin) has not run yet");
        }
        return group;
    }
}
