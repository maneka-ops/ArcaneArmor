package me.arcane.armor.api.rune;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import me.arcane.armor.api.armor.SetTrait;
import me.arcane.armor.api.registry.Keyed;
import org.bukkit.ChatColor;
import org.bukkit.Material;

/**
 * A permanent engraving applied to a piece of armor at the Rune Engraver.
 *
 * <p>Runes differ from modules in two ways that matter for balance: they cannot be removed, and a
 * piece accepts far fewer of them ({@link me.arcane.armor.api.armor.ArmorTier#getRuneSlots()}).
 * They are the commitment layer on top of the swappable module layer.
 */
public final class Rune implements Keyed {

    private final String id;
    private final String displayName;
    private final String description;
    private final Material icon;
    private final ChatColor color;
    private final Set<SetTrait> traits;
    private final double damageReduction;
    private final double damageBonus;
    private final int bonusEnergyCapacity;

    private Rune(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.icon = builder.icon;
        this.color = builder.color;
        this.traits = builder.traits.isEmpty()
                ? Collections.unmodifiableSet(EnumSet.noneOf(SetTrait.class))
                : Collections.unmodifiableSet(EnumSet.copyOf(builder.traits));
        this.damageReduction = builder.damageReduction;
        this.damageBonus = builder.damageBonus;
        this.bonusEnergyCapacity = builder.bonusEnergyCapacity;
    }

    @Nonnull
    public static Builder builder(@Nonnull String id, @Nonnull String displayName) {
        return new Builder(id, displayName);
    }

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    @Nonnull
    public String getDisplayName() {
        return displayName;
    }

    @Nonnull
    public String getDescription() {
        return description;
    }

    @Nonnull
    public Material getIcon() {
        return icon;
    }

    @Nonnull
    public ChatColor getColor() {
        return color;
    }

    @Nonnull
    public Set<SetTrait> getTraits() {
        return traits;
    }

    /** Fraction of incoming damage removed, 0.0 - 0.5. */
    public double getDamageReduction() {
        return damageReduction;
    }

    /** Additional outgoing melee damage multiplier. */
    public double getDamageBonus() {
        return damageBonus;
    }

    public int getBonusEnergyCapacity() {
        return bonusEnergyCapacity;
    }

    /** Slimefun item id of the physical rune item. */
    @Nonnull
    public String getItemId() {
        return "ARCANE_RUNE_" + id.toUpperCase();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Rune rune && rune.id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Rune[" + id + "]";
    }

    /** Fluent builder. */
    public static final class Builder {

        private final String id;
        private final String displayName;
        private final Set<SetTrait> traits = EnumSet.noneOf(SetTrait.class);
        private String description = "";
        private Material icon = Material.ECHO_SHARD;
        private ChatColor color = ChatColor.DARK_AQUA;
        private double damageReduction;
        private double damageBonus;
        private int bonusEnergyCapacity;

        private Builder(String id, String displayName) {
            this.id = Objects.requireNonNull(id, "id").toLowerCase();
            this.displayName = Objects.requireNonNull(displayName, "displayName");
        }

        @Nonnull
        public Builder description(@Nonnull String description) {
            this.description = description;
            return this;
        }

        @Nonnull
        public Builder icon(@Nonnull Material icon, @Nonnull ChatColor color) {
            this.icon = icon;
            this.color = color;
            return this;
        }

        @Nonnull
        public Builder trait(@Nonnull SetTrait... values) {
            Collections.addAll(this.traits, values);
            return this;
        }

        @Nonnull
        public Builder damageReduction(double fraction) {
            if (fraction < 0.0D || fraction > 0.5D) {
                throw new IllegalArgumentException("damage reduction must be within 0.0 - 0.5");
            }
            this.damageReduction = fraction;
            return this;
        }

        @Nonnull
        public Builder damageBonus(double fraction) {
            this.damageBonus = fraction;
            return this;
        }

        @Nonnull
        public Builder energyCapacity(int bonus) {
            this.bonusEnergyCapacity = bonus;
            return this;
        }

        @Nonnull
        public Rune build() {
            return new Rune(this);
        }
    }
}
