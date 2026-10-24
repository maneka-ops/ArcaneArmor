package me.arcane.armor.api.gear;

import javax.annotation.Nonnull;
import org.bukkit.Material;

/**
 * Weapons and tools an Arcane set can be forged into, alongside its armor.
 *
 * <p>Each type carries the vanilla base it is built on and the stat profile the factory scales by
 * tier. Damage and speed live here rather than in the factory so adding a sixth type is a matter of
 * adding a constant.
 */
public enum ArcaneGearType {

    SWORD(Material.NETHERITE_SWORD, "Blade", 12.0D, -2.0D),
    AXE(Material.NETHERITE_AXE, "Axe", 14.0D, -2.6D),
    PICKAXE(Material.NETHERITE_PICKAXE, "Pickaxe", 7.0D, -2.4D),
    SHIELD(Material.SHIELD, "Bulwark", 0.0D, 0.0D),
    BOW(Material.BOW, "Longbow", 0.0D, 0.0D);

    private final Material material;
    private final String displayName;
    private final double baseDamage;
    private final double attackSpeed;

    ArcaneGearType(Material material, String displayName, double baseDamage, double attackSpeed) {
        this.material = material;
        this.displayName = displayName;
        this.baseDamage = baseDamage;
        this.attackSpeed = attackSpeed;
    }

    @Nonnull
    public Material getMaterial() {
        return material;
    }

    @Nonnull
    public String getDisplayName() {
        return displayName;
    }

    /** Bonus attack damage before the tier multiplier; zero for gear that is not swung. */
    public double getBaseDamage() {
        return baseDamage;
    }

    /** Attack speed modifier, kept negative so the vanilla feel of a heavy weapon survives. */
    public double getAttackSpeed() {
        return attackSpeed;
    }

    public boolean isMelee() {
        return baseDamage > 0.0D;
    }

    /** Slimefun item id, e.g. {@code ARCANE_STORM_SWORD}. */
    @Nonnull
    public String getItemId(@Nonnull String setId) {
        return "ARCANE_" + setId.toUpperCase() + "_" + name();
    }
}
