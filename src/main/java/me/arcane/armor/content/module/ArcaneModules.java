package me.arcane.armor.content.module;

import java.util.List;
import javax.annotation.Nonnull;
import me.arcane.armor.api.armor.SetTrait;
import me.arcane.armor.api.module.ArmorModule;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

/**
 * The bundled module catalogue.
 *
 * <p>Modules are pure data, so this file is the whole feature: adding one here makes it craftable,
 * installable, and active in the tick pass without a line of behaviour code anywhere else.
 */
public final class ArcaneModules {

    private ArcaneModules() {
    }

    public static final ArmorModule MINING = ArmorModule.builder("mining", "Mining Module")
            .description("Permanent Haste while worn.")
            .icon(Material.DIAMOND_PICKAXE, ChatColor.AQUA)
            .effect(PotionEffectType.HASTE, 1)
            .build();

    public static final ArmorModule EXPLORER = ArmorModule.builder("explorer", "Explorer Module")
            .description("Permanent Speed while worn.")
            .icon(Material.LEATHER_BOOTS, ChatColor.GREEN)
            .effect(PotionEffectType.SPEED, 1)
            .build();

    public static final ArmorModule COMBAT = ArmorModule.builder("combat", "Combat Module")
            .description("Permanent Strength and heavier melee hits.")
            .icon(Material.IRON_SWORD, ChatColor.RED)
            .effect(PotionEffectType.STRENGTH, 0)
            .damageBonus(0.10D)
            .build();

    public static final ArmorModule TANK = ArmorModule.builder("tank", "Tank Module")
            .description("Permanent Resistance and heavy knockback reduction.")
            .icon(Material.SHIELD, ChatColor.GRAY)
            .effect(PotionEffectType.RESISTANCE, 0)
            .trait(SetTrait.KNOCKBACK_RESIST)
            .build();

    public static final ArmorModule FIRE = ArmorModule.builder("fire", "Fire Module")
            .description("Fire, lava and burning cannot touch you.")
            .icon(Material.MAGMA_CREAM, ChatColor.GOLD)
            .trait(SetTrait.FIRE_IMMUNE)
            .build();

    public static final ArmorModule WATER = ArmorModule.builder("water", "Water Module")
            .description("Breathe underwater indefinitely.")
            .icon(Material.HEART_OF_THE_SEA, ChatColor.BLUE)
            .effect(PotionEffectType.WATER_BREATHING, 0)
            .effect(PotionEffectType.CONDUIT_POWER, 0)
            .build();

    public static final ArmorModule JUMP = ArmorModule.builder("jump", "Jump Module")
            .description("Jump Boost, and the fall no longer hurts.")
            .icon(Material.RABBIT_FOOT, ChatColor.LIGHT_PURPLE)
            .effect(PotionEffectType.JUMP_BOOST, 1)
            .trait(SetTrait.FALL_IMMUNE)
            .build();

    public static final ArmorModule REGENERATION = ArmorModule.builder("regeneration", "Regeneration Module")
            .description("Slow, permanent health regeneration.")
            .icon(Material.GHAST_TEAR, ChatColor.LIGHT_PURPLE)
            .effect(PotionEffectType.REGENERATION, 0)
            .build();

    public static final ArmorModule LIFE_STEAL = ArmorModule.builder("life_steal", "Life Steal Module")
            .description("Melee damage heals a fraction back.")
            .icon(Material.REDSTONE, ChatColor.DARK_RED)
            .trait(SetTrait.LIFE_STEAL)
            .build();

    public static final ArmorModule LUCKY = ArmorModule.builder("lucky", "Lucky Module")
            .description("Extra loot rolls and better luck.")
            .icon(Material.EMERALD, ChatColor.GREEN)
            .effect(PotionEffectType.LUCK, 0)
            .trait(SetTrait.LUCK)
            .build();

    public static final ArmorModule ENERGY_SAVER = ArmorModule.builder("energy_saver", "Energy Saver Module")
            .description("Abilities cost 25% less Arcane Energy.")
            .icon(Material.REPEATER, ChatColor.AQUA)
            .energyDiscount(0.25D)
            .build();

    public static final ArmorModule CRITICAL = ArmorModule.builder("critical", "Critical Module")
            .description("Critical hits land considerably harder.")
            .icon(Material.NETHERITE_SWORD, ChatColor.DARK_RED)
            .trait(SetTrait.CRIT_BONUS)
            .damageBonus(0.05D)
            .build();

    public static final ArmorModule MAGNET = ArmorModule.builder("magnet", "Magnet Module")
            .description("Nearby dropped items come to you.")
            .icon(Material.HOPPER, ChatColor.GRAY)
            .trait(SetTrait.MAGNET)
            .build();

    public static final ArmorModule NIGHT_VISION = ArmorModule.builder("night_vision", "Night Vision Module")
            .description("You always see in the dark.")
            .icon(Material.GOLDEN_CARROT, ChatColor.YELLOW)
            .effect(PotionEffectType.NIGHT_VISION, 0)
            .build();

    /** Every bundled module, in guide order. */
    @Nonnull
    public static List<ArmorModule> all() {
        return List.of(
                MINING, EXPLORER, COMBAT, TANK, FIRE, WATER, JUMP, REGENERATION,
                LIFE_STEAL, LUCKY, ENERGY_SAVER, CRITICAL, MAGNET, NIGHT_VISION);
    }
}
