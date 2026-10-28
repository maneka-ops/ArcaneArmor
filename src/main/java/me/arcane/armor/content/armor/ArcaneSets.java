package me.arcane.armor.content.armor;

import java.util.List;
import javax.annotation.Nonnull;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.api.armor.PassiveEffect;
import me.arcane.armor.api.armor.SetTrait;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

/**
 * The bundled armor sets.
 *
 * <p>Each set is written to answer one question: what does wearing this let me do that the others
 * do not? Every set shares a strong baseline — see {@link #baseline} — and then pushes hard in its
 * own direction on top of it, so the choice between sets is about what you want to be good at
 * rather than about which one is stronger.
 *
 * <p>Overlapping effects do not stack; the tick task keeps the highest amplifier from any source,
 * so a set listing Speed III simply overrides the baseline's Speed II.
 *
 * <p>The tier unlock ladder lives on top of these definitions: Tier II adds Aqua Affinity and
 * Tier III Swift Sneak in {@code ArmorItemFactory}, Tier IV raises every passive one amplifier in
 * {@code ArcaneTickTask}, Tier V grants the set's Tier V signature trait, and Ascended wearers
 * keep their armor on death through {@code SetTrait.ARCANE_BOND}.
 */
public final class ArcaneSets {

    private ArcaneSets() {
    }

    /**
     * Effects every Arcane set grants, so any piece is worth wearing on its own.
     *
     * <p>Water breathing comes per piece because it is quality-of-life rather than power; the
     * combat and movement effects wait for the full set.
     */
    @Nonnull
    private static ArmorSetDefinition.Builder baseline(@Nonnull ArmorSetDefinition.Builder builder) {
        return builder
                .passive(PassiveEffect.perPiece(PotionEffectType.WATER_BREATHING, 0))
                .passive(PassiveEffect.fullSet(PotionEffectType.SPEED, 0))
                .passive(PassiveEffect.fullSet(PotionEffectType.REGENERATION, 0));
    }

    /** Entry set: cheap, mana-flavoured, and the one every other set is crafted through. */
    public static final ArmorSetDefinition APPRENTICE = baseline(
            ArmorSetDefinition.builder("apprentice", "Apprentice"))
            .enchantBase(2)
            .color(ChatColor.LIGHT_PURPLE, Color.fromRGB(150, 100, 220))
            .theme(Material.AMETHYST_SHARD)
            .passive(PassiveEffect.fullSet(PotionEffectType.REGENERATION, 1))
            .passive(PassiveEffect.fullSet(PotionEffectType.LUCK, 0))
            .trait(SetTrait.XP_BONUS)
            .signature(SetTrait.MANA_SPRING)
            .active("mana_wave")
            .ultimate("mana_burst")
            .energy(1_000)
            .defense(3.0D, 1.0D)
            .build();

    /** Mobility and burst damage; the set that punishes standing still. */
    public static final ArmorSetDefinition STORM = baseline(
            ArmorSetDefinition.builder("storm", "Storm"))
            .enchantBase(3)
            .upgradeFrom("apprentice")
            .color(ChatColor.YELLOW, Color.fromRGB(255, 255, 255))
            .theme(Material.LIGHTNING_ROD)
            // Storm's edge is movement, not overwhelming output: the baseline Speed I carries the
            // mobility, the extra passives are the single-tier quality-of-life pair, and the
            // damage traits fire on a leash rather than on every single hit.
            .passive(PassiveEffect.fullSet(PotionEffectType.HASTE, 0))
            .passive(PassiveEffect.fullSet(PotionEffectType.REGENERATION, 1))
            .trait(SetTrait.LIGHTNING_IMMUNE, SetTrait.CHAIN_LIGHTNING)
            .signature(SetTrait.THUNDER_STEP)
            .active("lightning_strike")
            .ultimate("thunderstorm")
            .energy(1_400)
            .defense(3.0D, 1.0D)
            .build();

    /** Nether traversal and sustained burn damage. */
    public static final ArmorSetDefinition INFERNAL = baseline(
            ArmorSetDefinition.builder("infernal", "Infernal"))
            .enchantBase(4)
            .upgradeFrom("storm")
            .color(ChatColor.RED, Color.fromRGB(200, 60, 30))
            .theme(Material.BLAZE_ROD)
            .passive(PassiveEffect.fullSet(PotionEffectType.FIRE_RESISTANCE, 0))
            .passive(PassiveEffect.fullSet(PotionEffectType.STRENGTH, 0))
            .passive(PassiveEffect.fullSet(PotionEffectType.REGENERATION, 1))
            .trait(SetTrait.FIRE_IMMUNE, SetTrait.LAVA_WALK, SetTrait.AUTO_SMELT)
            .signature(SetTrait.MOLTEN_EDGE)
            .active("flame_spin")
            .ultimate("inferno_explosion")
            .energy(1_600)
            .defense(4.0D, 2.0D)
            .build();

    /** Control: slows, freezes and punishes anything that closes the distance. */
    public static final ArmorSetDefinition FROST = baseline(
            ArmorSetDefinition.builder("frost", "Frost"))
            .enchantBase(6)
            .upgradeFrom("infernal")
            .color(ChatColor.AQUA, Color.fromRGB(140, 220, 240))
            .theme(Material.BLUE_ICE)
            .passive(PassiveEffect.fullSet(PotionEffectType.RESISTANCE, 1))
            .passive(PassiveEffect.fullSet(PotionEffectType.REGENERATION, 1))
            .passive(PassiveEffect.fullSet(PotionEffectType.JUMP_BOOST, 1))
            .trait(SetTrait.FREEZE_ATTACKERS, SetTrait.SNOW_WALKER)
            .signature(SetTrait.GLACIAL_SHELL)
            .active("ice_nova")
            .ultimate("blizzard")
            .energy(1_600)
            .defense(5.0D, 3.0D)
            .build();

    /** Repositioning: the set built around getting somewhere else, fast. */
    public static final ArmorSetDefinition VOID = baseline(
            ArmorSetDefinition.builder("void", "Void"))
            .enchantBase(9)
            .upgradeFrom("frost")
            .color(ChatColor.DARK_PURPLE, Color.fromRGB(80, 40, 130))
            .theme(Material.ENDER_EYE)
            .passive(PassiveEffect.fullSet(PotionEffectType.SPEED, 1))
            .passive(PassiveEffect.fullSet(PotionEffectType.JUMP_BOOST, 1))
            .trait(SetTrait.FALL_IMMUNE, SetTrait.ENDERMAN_PACIFY, SetTrait.TELEPORT_RANGE)
            .signature(SetTrait.PHASE_WALK)
            .active("blink")
            .ultimate("void_rift")
            .energy(1_800)
            .defense(4.5D, 2.5D)
            .build();

    /** Every bundled set, in progression order. */
    @Nonnull
    public static List<ArmorSetDefinition> all() {
        return List.of(APPRENTICE, STORM, INFERNAL, FROST, VOID);
    }
}
