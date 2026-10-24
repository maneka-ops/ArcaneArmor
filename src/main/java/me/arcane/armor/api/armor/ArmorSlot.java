package me.arcane.armor.api.armor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * The four armor slots an Arcane set covers, plus the vanilla materials each tier is built on.
 */
public enum ArmorSlot {

    HELMET(EquipmentSlot.HEAD, Material.LEATHER_HELMET, Material.NETHERITE_HELMET, "Helmet"),
    CHESTPLATE(EquipmentSlot.CHEST, Material.LEATHER_CHESTPLATE, Material.NETHERITE_CHESTPLATE, "Chestplate"),
    LEGGINGS(EquipmentSlot.LEGS, Material.LEATHER_LEGGINGS, Material.NETHERITE_LEGGINGS, "Leggings"),
    BOOTS(EquipmentSlot.FEET, Material.LEATHER_BOOTS, Material.NETHERITE_BOOTS, "Boots");

    private final EquipmentSlot equipmentSlot;
    private final Material dyeableMaterial;
    private final Material endgameMaterial;
    private final String displayName;

    ArmorSlot(EquipmentSlot equipmentSlot, Material dyeableMaterial, Material endgameMaterial, String displayName) {
        this.equipmentSlot = equipmentSlot;
        this.dyeableMaterial = dyeableMaterial;
        this.endgameMaterial = endgameMaterial;
        this.displayName = displayName;
    }

    @Nonnull
    public EquipmentSlot getEquipmentSlot() {
        return equipmentSlot;
    }

    /** Leather base, so a set can be tinted with its theme colour. */
    @Nonnull
    public Material getDyeableMaterial() {
        return dyeableMaterial;
    }

    @Nonnull
    public Material getEndgameMaterial() {
        return endgameMaterial;
    }

    @Nonnull
    public String getDisplayName() {
        return displayName;
    }

    /** Reads whatever the player currently wears in this slot. */
    @Nullable
    public ItemStack getWornItem(@Nonnull Player player) {
        return player.getInventory().getItem(equipmentSlot);
    }

    public void setWornItem(@Nonnull Player player, @Nullable ItemStack item) {
        player.getInventory().setItem(equipmentSlot, item);
    }
}
