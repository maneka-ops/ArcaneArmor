package me.arcane.armor.content.gear;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.api.armor.ArmorTier;
import me.arcane.armor.api.gear.ArcaneGearType;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Builds the weapon and tool line that matches each armor set.
 *
 * <p>Gear is deliberately simpler than armor: no modules, no runes, and the only stored state is its
 * tier, which the Upgrade Altar raises exactly like an armor piece's. What it does
 * carry is the set's identity — a Storm blade hits like lightning, an Infernal axe sets things on
 * fire — expressed entirely through enchantments and attribute modifiers, so nothing has to tick to
 * keep it working.
 */
public final class ArcaneGearFactory {

    /** Level cap these items are built around, matching the armor line. */
    private static final int MAX_LEVEL = 9;

    /** Separate, much lower cap for Thorns, matching the armor line. */
    private static final int MAX_THORNS = 4;

    private final Plugin plugin;
    private final NamespacedKey tierKey;

    public ArcaneGearFactory(@Nonnull Plugin plugin) {
        this.plugin = plugin;
        this.tierKey = new NamespacedKey(plugin, "gear_tier");
    }

    @Nonnull
    public SlimefunItemStack createItem(
            @Nonnull ArmorSetDefinition set,
            @Nonnull ArcaneGearType type,
            @Nonnull ArmorTier tier) {

        ItemStack base = new ItemStack(type.getMaterial());
        ItemMeta meta = base.getItemMeta();
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        applyTier(meta, set, type, tier);
        base.setItemMeta(meta);
        return new SlimefunItemStack(type.getItemId(set.getId()), base);
    }

    /**
     * Tier stored on a gear item. Gear forged before tiers were stored has none and reads as the
     * set's starting tier, which is what it was built at.
     */
    @Nonnull
    public ArmorTier readTier(@Nonnull ItemStack item, @Nonnull ArmorTier fallback) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return fallback;
        }
        return ArmorTier.fromId(meta.getPersistentDataContainer().get(tierKey, PersistentDataType.STRING), fallback);
    }

    /** Moves an existing gear item to a new tier, rebuilding its name, lore, enchants and stats. */
    public void setTier(
            @Nonnull ItemStack item,
            @Nonnull ArmorSetDefinition set,
            @Nonnull ArcaneGearType type,
            @Nonnull ArmorTier tier) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        applyTier(meta, set, type, tier);
        item.setItemMeta(meta);
    }

    private void applyTier(
            @Nonnull ItemMeta meta,
            @Nonnull ArmorSetDefinition set,
            @Nonnull ArcaneGearType type,
            @Nonnull ArmorTier tier) {

        meta.setDisplayName(set.getColor() + set.getDisplayName() + " " + type.getDisplayName()
                + " " + ChatColor.DARK_GRAY + "[" + tier.getColor() + tier.getLabel() + ChatColor.DARK_GRAY + "]");
        meta.setLore(buildLore(set, type, tier));

        // Cleared first so an upgrade replaces the old levels instead of keeping the higher of two.
        for (Enchantment enchantment : new ArrayList<>(meta.getEnchants().keySet())) {
            meta.removeEnchant(enchantment);
        }
        applyEnchantments(meta, set, type, Math.min(MAX_LEVEL, set.getEnchantBase() + tier.ordinal()));
        applyAttributes(meta, type, tier);

        meta.getPersistentDataContainer().set(tierKey, PersistentDataType.STRING, tier.name());
    }

    @Nonnull
    private List<String> buildLore(
            @Nonnull ArmorSetDefinition set,
            @Nonnull ArcaneGearType type,
            @Nonnull ArmorTier tier) {

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Set: " + set.getColor() + set.getDisplayName());
        lore.add(ChatColor.GRAY + "Tier: " + tier.getColor() + tier.getLabel());

        if (type.isMelee()) {
            lore.add(ChatColor.GRAY + "Damage: " + ChatColor.RED
                    + String.format("+%.1f", type.getBaseDamage() * tier.getStatMultiplier()));
        }

        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Pairs with the " + set.getDisplayName() + " set.");
        if (!tier.isMax()) {
            lore.add(ChatColor.DARK_GRAY + "Upgrade at the Upgrade Altar.");
        }
        lore.add(ChatColor.DARK_PURPLE + "Arcane Gear");
        return lore;
    }

    /**
     * Enchants by role first, then by set.
     *
     * <p>Levels ignore the vanilla ceiling on purpose; this is endgame gear built to sit at the top
     * of a five-set ladder.
     */
    private void applyEnchantments(
            @Nonnull ItemMeta meta,
            @Nonnull ArmorSetDefinition set,
            @Nonnull ArcaneGearType type,
            int level) {

        meta.addEnchant(Enchantment.UNBREAKING, MAX_LEVEL, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);

        switch (type) {
            case SWORD -> {
                meta.addEnchant(Enchantment.SHARPNESS, level, true);
                meta.addEnchant(Enchantment.LOOTING, 4, true);
                meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
                meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
                meta.addEnchant(Enchantment.KNOCKBACK, 2, true);
            }
            case AXE -> {
                meta.addEnchant(Enchantment.SHARPNESS, level, true);
                meta.addEnchant(Enchantment.EFFICIENCY, level, true);
                meta.addEnchant(Enchantment.FORTUNE, 4, true);
                meta.addEnchant(Enchantment.LOOTING, 4, true);
            }
            case PICKAXE -> {
                meta.addEnchant(Enchantment.EFFICIENCY, level, true);
                meta.addEnchant(Enchantment.FORTUNE, 4, true);
            }
            case SHIELD -> meta.addEnchant(Enchantment.THORNS, Math.min(MAX_THORNS, level), true);
            case BOW -> {
                meta.addEnchant(Enchantment.POWER, level, true);
                meta.addEnchant(Enchantment.PUNCH, 2, true);
                meta.addEnchant(Enchantment.FLAME, 1, true);
                meta.addEnchant(Enchantment.INFINITY, 1, true);
            }
        }

        // Set flavour, applied on top so a Frost pickaxe still mines like an Arcane pickaxe.
        switch (set.getId()) {
            case "infernal" -> {
                if (type.isMelee()) {
                    meta.addEnchant(Enchantment.FIRE_ASPECT, 3, true);
                }
                if (type == ArcaneGearType.BOW) {
                    meta.addEnchant(Enchantment.FLAME, 1, true);
                }
            }
            case "storm" -> {
                if (type.isMelee()) {
                    meta.addEnchant(Enchantment.KNOCKBACK, 3, true);
                }
                if (type == ArcaneGearType.BOW) {
                    meta.addEnchant(Enchantment.PUNCH, Math.min(MAX_LEVEL, level), true);
                }
            }
            case "frost" -> meta.addEnchant(Enchantment.THORNS, MAX_THORNS, true);
            case "void" -> {
                if (type.isMelee() && type != ArcaneGearType.PICKAXE) {
                    meta.addEnchant(Enchantment.SHARPNESS, Math.min(MAX_LEVEL, level), true);
                }
            }
            case "apprentice" -> {
                if (type == ArcaneGearType.PICKAXE || type == ArcaneGearType.AXE) {
                    meta.addEnchant(Enchantment.FORTUNE, Math.min(MAX_LEVEL, level), true);
                }
            }
            default -> {
                // Sets added through the API keep the role enchantments only.
            }
        }
    }

    /** Attack damage and speed, scaled by tier. Only melee gear carries modifiers. */
    private void applyAttributes(
            @Nonnull ItemMeta meta,
            @Nonnull ArcaneGearType type,
            @Nonnull ArmorTier tier) {

        if (!type.isMelee()) {
            return;
        }

        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.ATTACK_SPEED);

        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                new NamespacedKey(plugin, "damage_" + type.name().toLowerCase()),
                type.getBaseDamage() * tier.getStatMultiplier(),
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND));

        meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
                new NamespacedKey(plugin, "speed_" + type.name().toLowerCase()),
                type.getAttackSpeed(),
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND));
    }
}
