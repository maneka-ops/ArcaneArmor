package me.arcane.armor.api.armor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;

/**
 * Upgrade ladder every Arcane set climbs.
 *
 * <p>A tier is a multiplier sheet rather than a stat sheet: sets keep their own identity and the
 * tier decides how far that identity scales, how many modules fit, and how much energy the piece
 * can hold. Ability cooldowns shrink as the tier rises, which is what makes the top tiers feel
 * different rather than merely bigger.
 */
public enum ArmorTier {

    TIER_1("I", 1, 1.0D, 1.00D, ChatColor.WHITE),
    TIER_2("II", 2, 1.25D, 0.92D, ChatColor.GREEN),
    TIER_3("III", 3, 1.55D, 0.84D, ChatColor.AQUA),
    TIER_4("IV", 4, 1.90D, 0.74D, ChatColor.LIGHT_PURPLE),
    TIER_5("V", 5, 2.30D, 0.62D, ChatColor.GOLD),
    ASCENDED("Ascended", 6, 3.00D, 0.45D, ChatColor.DARK_PURPLE);

    private static final ArmorTier[] ORDER = values();

    private final String label;
    private final int moduleSlots;
    private final double statMultiplier;
    private final double cooldownMultiplier;
    private final ChatColor color;

    ArmorTier(String label, int moduleSlots, double statMultiplier, double cooldownMultiplier, ChatColor color) {
        this.label = label;
        this.moduleSlots = moduleSlots;
        this.statMultiplier = statMultiplier;
        this.cooldownMultiplier = cooldownMultiplier;
        this.color = color;
    }

    @Nonnull
    public String getLabel() {
        return label;
    }

    /** How many modules a piece of this tier accepts. */
    public int getModuleSlots() {
        return moduleSlots;
    }

    /** How many runes a piece of this tier accepts; runes stay scarcer than modules on purpose. */
    public int getRuneSlots() {
        return Math.max(1, (ordinal() + 2) / 2);
    }

    public double getStatMultiplier() {
        return statMultiplier;
    }

    public double getCooldownMultiplier() {
        return cooldownMultiplier;
    }

    /** Energy capacity granted per piece at this tier. */
    public int getEnergyCapacity(int baseCapacity) {
        return (int) Math.round(baseCapacity * statMultiplier);
    }

    @Nonnull
    public ChatColor getColor() {
        return color;
    }

    public boolean isMax() {
        return this == ASCENDED;
    }

    /** @return the next tier, or {@code null} when already {@link #ASCENDED} */
    @Nullable
    public ArmorTier next() {
        return isMax() ? null : ORDER[ordinal() + 1];
    }

    @Nonnull
    public static ArmorTier fromId(@Nullable String id, @Nonnull ArmorTier fallback) {
        if (id == null || id.isEmpty()) {
            return fallback;
        }
        for (ArmorTier tier : ORDER) {
            if (tier.name().equalsIgnoreCase(id)) {
                return tier;
            }
        }
        return fallback;
    }
}
