package me.arcane.armor.content.rune;

import java.util.List;
import javax.annotation.Nonnull;
import me.arcane.armor.api.armor.SetTrait;
import me.arcane.armor.api.rune.Rune;
import org.bukkit.ChatColor;
import org.bukkit.Material;

/**
 * The bundled rune catalogue.
 *
 * <p>Runes are permanent, so their numbers are smaller than a module's: the trade is that they do
 * not occupy a module slot and cannot be swapped out when the fight changes.
 */
public final class ArcaneRunes {

    private ArcaneRunes() {
    }

    public static final Rune FIRE = Rune.builder("fire", "Rune of Fire")
            .description("Fire cannot harm the bearer, and each blow ignites.")
            .icon(Material.BLAZE_POWDER, ChatColor.RED)
            .trait(SetTrait.FIRE_IMMUNE, SetTrait.MOLTEN_EDGE)
            .damageBonus(0.05D)
            .build();

    public static final Rune ICE = Rune.builder("ice", "Rune of Ice")
            .description("Attackers are frozen where they stand.")
            .icon(Material.BLUE_ICE, ChatColor.AQUA)
            .trait(SetTrait.FREEZE_ATTACKERS, SetTrait.SNOW_WALKER)
            .build();

    public static final Rune WIND = Rune.builder("wind", "Rune of Wind")
            .description("The ground never breaks a fall.")
            .icon(Material.FEATHER, ChatColor.WHITE)
            .trait(SetTrait.FALL_IMMUNE)
            .build();

    public static final Rune EARTH = Rune.builder("earth", "Rune of Earth")
            .description("Blows land softer and push less.")
            .icon(Material.DEEPSLATE, ChatColor.DARK_GRAY)
            .trait(SetTrait.KNOCKBACK_RESIST)
            .damageReduction(0.08D)
            .build();

    public static final Rune VOID = Rune.builder("void", "Rune of Void")
            .description("Endermen look away, and pearls fly further.")
            .icon(Material.ENDER_EYE, ChatColor.DARK_PURPLE)
            .trait(SetTrait.ENDERMAN_PACIFY, SetTrait.TELEPORT_RANGE)
            .build();

    public static final Rune MANA = Rune.builder("mana", "Rune of Mana")
            .description("The piece holds noticeably more energy.")
            .icon(Material.LAPIS_LAZULI, ChatColor.BLUE)
            .energyCapacity(500)
            .build();

    public static final Rune SPEED = Rune.builder("speed", "Rune of Speed")
            .description("Footsteps fall silent and quick.")
            .icon(Material.SUGAR, ChatColor.YELLOW)
            .trait(SetTrait.SILENT_STEPS)
            .build();

    public static final Rune PROTECTION = Rune.builder("protection", "Rune of Protection")
            .description("A flat cut to all incoming damage.")
            .icon(Material.SHIELD, ChatColor.GRAY)
            .damageReduction(0.12D)
            .build();

    public static final Rune FORTUNE = Rune.builder("fortune", "Rune of Fortune")
            .description("Fortune favours the engraved.")
            .icon(Material.GOLD_INGOT, ChatColor.GOLD)
            .trait(SetTrait.LUCK)
            .build();

    public static final Rune VAMPIRISM = Rune.builder("vampirism", "Rune of Vampirism")
            .description("Damage dealt returns as health.")
            .icon(Material.GHAST_TEAR, ChatColor.DARK_RED)
            .trait(SetTrait.LIFE_STEAL)
            .damageBonus(0.05D)
            .build();

    /** Every bundled rune, in guide order. */
    @Nonnull
    public static List<Rune> all() {
        return List.of(FIRE, ICE, WIND, EARTH, VOID, MANA, SPEED, PROTECTION, FORTUNE, VAMPIRISM);
    }
}
