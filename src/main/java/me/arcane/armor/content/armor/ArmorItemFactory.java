package me.arcane.armor.content.armor;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import me.arcane.armor.api.ability.Ability;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.api.armor.ArmorSlot;
import me.arcane.armor.api.armor.ArmorTier;
import me.arcane.armor.api.registry.Registry;
import me.arcane.armor.core.data.ArmorData;
import me.arcane.armor.core.data.ArmorDataService;
import me.arcane.armor.config.ArcaneConfig;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

/**
 * Builds the physical item for one piece of one set at one tier.
 *
 * <p>The item carries everything a piece needs to be self-describing: attribute modifiers for its
 * defense, lore generated from the live registries, and the persistent data that makes it an Arcane
 * item at all. Nothing about a piece is stored plugin-side, so a piece that has been sitting in a
 * shulker for a month still works after an update.
 */
public final class ArmorItemFactory {

    private final Plugin plugin;
    private final ArmorDataService dataService;
    private final Registry<Ability> abilities;
    private final ArcaneConfig config;

    public ArmorItemFactory(
            @Nonnull Plugin plugin,
            @Nonnull ArmorDataService dataService,
            @Nonnull Registry<Ability> abilities,
            @Nonnull ArcaneConfig config) {
        this.plugin = plugin;
        this.dataService = dataService;
        this.abilities = abilities;
        this.config = config;
    }

    /**
     * Creates the Slimefun item stack registered in the guide for this set and slot.
     */
    @Nonnull
    public SlimefunItemStack createItem(
            @Nonnull ArmorSetDefinition set,
            @Nonnull ArmorSlot slot,
            @Nonnull ArmorTier tier,
            double defenseMultiplier) {

        ItemStack base = new ItemStack(materialFor(slot));
        ItemMeta meta = base.getItemMeta();

        String name = set.getColor() + set.getDisplayName() + " " + slot.getDisplayName()
                + " " + ChatColor.DARK_GRAY + "[" + tier.getColor() + tier.getLabel() + ChatColor.DARK_GRAY + "]";
        meta.setDisplayName(name);
        meta.setLore(buildLore(set, slot, tier));
        meta.setUnbreakable(true);
        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_DYE);

        applyDefense(meta, set, slot, tier, defenseMultiplier);
        applyEnchantments(meta, set, slot, tier);
        base.setItemMeta(meta);

        // Stamp the starting Arcane state before the stack is frozen into a Slimefun item, so the
        // very first piece a player crafts already has a tier, a set and an empty module list.
        dataService.write(base, new ArmorData(set.getId(), tier, 0, List.of(), List.of()));

        return new SlimefunItemStack(set.getItemId(slot), base);
    }

    /**
     * Rebuilds an existing piece's presentation after its data changed, keeping the stored state.
     * Used by the Upgrade Altar, the Module Manager and the Rune Engraver.
     */
    public void refreshPresentation(
            @Nonnull ItemStack item,
            @Nonnull ArmorSetDefinition set,
            @Nonnull ArmorSlot slot,
            @Nonnull ArmorData data,
            double defenseMultiplier) {

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.setDisplayName(set.getColor() + set.getDisplayName() + " " + slot.getDisplayName()
                + " " + ChatColor.DARK_GRAY + "[" + data.tier().getColor() + data.tier().getLabel()
                + ChatColor.DARK_GRAY + "]");
        meta.setLore(buildLore(set, slot, data.tier()));

        // Attribute modifiers are keyed, so re-applying replaces rather than stacks.
        applyDefense(meta, set, slot, data.tier(), defenseMultiplier);
        applyEnchantments(meta, set, slot, data.tier());
        item.setItemMeta(meta);
    }

    @Nonnull
    private List<String> buildLore(
            @Nonnull ArmorSetDefinition set,
            @Nonnull ArmorSlot slot,
            @Nonnull ArmorTier tier) {

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Set: " + set.getColor() + set.getDisplayName());
        lore.add(ChatColor.GRAY + "Tier: " + tier.getColor() + tier.getLabel());
        lore.add(ChatColor.GRAY + "Protection: " + ChatColor.WHITE
                + roman(protectionLevel(set, tier)));

        // The energy line only earns its space when energy is actually switched on.
        if (config.isEnergyEnabled()) {
            lore.add(ChatColor.GRAY + "Energy: " + ChatColor.AQUA + "0"
                    + ChatColor.DARK_GRAY + "/" + ChatColor.AQUA
                    + set.effectiveTier(tier).getEnergyCapacity(set.getBaseEnergyCapacity()));
        }

        lore.add("");
        lore.add(ChatColor.GRAY + "Modules: " + ChatColor.WHITE + "0"
                + ChatColor.DARK_GRAY + "/" + ChatColor.WHITE + tier.getModuleSlots());
        lore.add(ChatColor.GRAY + "Runes: " + ChatColor.WHITE + "0"
                + ChatColor.DARK_GRAY + "/" + ChatColor.WHITE + tier.getRuneSlots());

        Ability active = abilities.get(set.getActiveAbilityId());
        Ability ultimate = abilities.get(set.getUltimateAbilityId());
        if (active != null || ultimate != null) {
            lore.add("");
            if (active != null) {
                lore.add(ChatColor.YELLOW + "Active: " + ChatColor.WHITE + active.getDisplayName());
            }
            if (ultimate != null) {
                lore.add(ChatColor.GOLD + "Ultimate: " + ChatColor.WHITE + ultimate.getDisplayName()
                        + ChatColor.DARK_GRAY + " (full set)");
            }
        }

        lore.add("");
        lore.add(ChatColor.DARK_PURPLE + "Arcane Armor");

        return lore;
    }

    private void applyDefense(
            @Nonnull ItemMeta meta,
            @Nonnull ArmorSetDefinition set,
            @Nonnull ArmorSlot slot,
            @Nonnull ArmorTier tier,
            double defenseMultiplier) {

        ArmorTier effective = set.effectiveTier(tier);
        double armor = set.getArmorPerPiece() * effective.getStatMultiplier() * defenseMultiplier;
        double toughness = set.getToughnessPerPiece() * effective.getStatMultiplier() * defenseMultiplier;
        EquipmentSlotGroup group = groupFor(slot);

        meta.removeAttributeModifier(Attribute.ARMOR);
        meta.removeAttributeModifier(Attribute.ARMOR_TOUGHNESS);

        meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(
                new NamespacedKey(plugin, "armor_" + slot.name().toLowerCase()),
                armor,
                AttributeModifier.Operation.ADD_NUMBER,
                group));

        meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(
                new NamespacedKey(plugin, "toughness_" + slot.name().toLowerCase()),
                toughness,
                AttributeModifier.Operation.ADD_NUMBER,
                group));
    }

    /**
     * Enchants a piece: Protection scaled by tier, Unbreaking, and whatever the set's theme implies.
     *
     * <p>Levels go past the vanilla ceiling on purpose — a tier-five piece carries Protection X — so
     * everything is added with the level restriction ignored. Re-applying is safe because each
     * enchantment is removed first, which matters when the Upgrade Altar rebuilds a piece in place.
     */
    private void applyEnchantments(
            @Nonnull ItemMeta meta,
            @Nonnull ArmorSetDefinition set,
            @Nonnull ArmorSlot slot,
            @Nonnull ArmorTier tier) {

        for (Enchantment enchantment : new ArrayList<>(meta.getEnchants().keySet())) {
            meta.removeEnchant(enchantment);
        }

        meta.addEnchant(Enchantment.PROTECTION, protectionLevel(set, tier), true);
        meta.addEnchant(Enchantment.UNBREAKING, MAX_LEVEL, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);

        int level = protectionLevel(set, tier);

        // The set's own damage family sits one level above the rest: enough to read on the tooltip,
        // not enough to make the other families irrelevant.
        int signature = Math.min(MAX_LEVEL, level + 1);

        // Every piece covers the three damage families (blast protection is deliberately left
        // out, so explosions keep their bite).
        meta.addEnchant(Enchantment.FIRE_PROTECTION, level, true);
        meta.addEnchant(Enchantment.PROJECTILE_PROTECTION, level, true);
        // Thorns is capped well below the others: it reflects damage the wearer never has to
        // aim, and a full set rolls the proc once per piece, so four Thorns-IV pieces could
        // shred an attacker with no input at all. The shared level stays low; only Frost, the
        // tank set, buys one level of it back. Storm keeps the shared Thorns too — but a proc
        // stays bare vanilla reflection: no set bonuses ride on it (ArcaneCombatListener).
        meta.addEnchant(Enchantment.THORNS, Math.min(SHARED_THORNS, level), true);

        if (slot == ArmorSlot.HELMET) {
            meta.addEnchant(Enchantment.RESPIRATION, 3, true);
        }
        if (slot == ArmorSlot.BOOTS) {
            meta.addEnchant(Enchantment.FEATHER_FALLING, Math.min(4, level), true);
            meta.addEnchant(Enchantment.DEPTH_STRIDER, 3, true);
        }

        // Tier unlock ladder: Tier II adds underwater mining to every piece, Tier III teaches the
        // leggings Swift Sneak. Both are added after the set switch, which is why an Apprentice
        // piece carries AQUA_AFFINITY from its set line as well - addEnchant is idempotent.
        if (tier.ordinal() >= ArmorTier.TIER_2.ordinal()) {
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
        }
        if (slot == ArmorSlot.LEGGINGS && tier.ordinal() >= ArmorTier.TIER_3.ordinal()) {
            meta.addEnchant(Enchantment.SWIFT_SNEAK, 2, true);
        }

        switch (set.getId()) {
            case "infernal" -> meta.addEnchant(Enchantment.FIRE_PROTECTION, signature, true);
            case "storm" -> meta.addEnchant(Enchantment.PROJECTILE_PROTECTION, signature, true);
            case "frost" -> {
                // Frost is the set that shrugs hits off, so its signature is ordinary Protection.
                meta.addEnchant(Enchantment.PROTECTION, signature, true);
                meta.addEnchant(Enchantment.THORNS, SHARED_THORNS + 1, true);
                if (slot == ArmorSlot.BOOTS) {
                    meta.addEnchant(Enchantment.FROST_WALKER, 2, true);
                }
            }
            case "void" -> {
                meta.addEnchant(Enchantment.PROJECTILE_PROTECTION, signature, true);
                if (slot == ArmorSlot.BOOTS) {
                    meta.addEnchant(Enchantment.FEATHER_FALLING, signature, true);
                }
            }
            case "apprentice" -> meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            default -> {
                // Sets added through the API keep the shared enchantments only.
            }
        }
    }

    private static final String[] NUMERALS =
            {"0", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    @Nonnull
    private static String roman(int level) {
        return level >= 0 && level < NUMERALS.length ? NUMERALS[level] : String.valueOf(level);
    }

    /**
     * Level cap for every enchantment on Arcane armor.
     *
     * <p>Deliberately nine rather than ten. Nothing in the addon reaches the round number, which
     * keeps a full endgame set strong but still short of absolute — there is always a hit that gets
     * through, and gear from other addons is not made pointless.
     */
    private static final int MAX_LEVEL = 9;

    /**
     * The shared Thorns cap.
     *
     * <p>Deliberately low: unlike the protections, Thorns needs no attention or aim, and a full
     * set rolls its proc once per piece, so the reflect damage multiplies through the whole set
     * without the wearer doing anything. Two keeps it a passive stinger rather than free combat.
     */
    private static final int SHARED_THORNS = 2;

    /**
     * Protection for a piece: the set's own floor plus its tier, capped at ten.
     *
     * <p>Two dials rather than one. The set decides where a piece starts — the early sets sit low so
     * there is somewhere to climb from — and the tier decides how far it has come. An endgame set
     * with a base of ten is at Protection X from its first tier, which is the point of reaching it.
     */
    private static int protectionLevel(@Nonnull ArmorSetDefinition set, @Nonnull ArmorTier tier) {
        return Math.min(MAX_LEVEL, set.getEnchantBase() + tier.ordinal());
    }

    /**
     * Every tier is netherite. Sets are told apart by their name colour and lore rather than by the
     * base material, so a tier-one piece already looks like endgame gear.
     */
    @Nonnull
    private static Material materialFor(@Nonnull ArmorSlot slot) {
        return slot.getEndgameMaterial();
    }

    @Nonnull
    private static EquipmentSlotGroup groupFor(@Nonnull ArmorSlot slot) {
        return switch (slot) {
            case HELMET -> EquipmentSlotGroup.HEAD;
            case CHESTPLATE -> EquipmentSlotGroup.CHEST;
            case LEGGINGS -> EquipmentSlotGroup.LEGS;
            case BOOTS -> EquipmentSlotGroup.FEET;
        };
    }
}
