package me.arcane.armor.api.module;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import me.arcane.armor.api.armor.SetTrait;
import me.arcane.armor.api.registry.Keyed;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

/**
 * A slotted upgrade the player installs into a piece of Arcane armor.
 *
 * <p>Modules are the customisation layer: two players wearing the same set at the same tier play
 * differently because of what they slotted. Like sets they are pure data, so the tick task can
 * fold every installed module into one effect pass without knowing what any of them are.
 */
public final class ArmorModule implements Keyed {

    private final String id;
    private final String displayName;
    private final String description;
    private final Material icon;
    private final ChatColor color;
    private final Map<PotionEffectType, Integer> effects;
    private final Set<SetTrait> traits;
    private final double energyDiscount;
    private final double damageBonus;

    private ArmorModule(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.icon = builder.icon;
        this.color = builder.color;
        this.effects = Map.copyOf(builder.effects);
        this.traits = builder.traits.isEmpty()
                ? Collections.unmodifiableSet(EnumSet.noneOf(SetTrait.class))
                : Collections.unmodifiableSet(EnumSet.copyOf(builder.traits));
        this.energyDiscount = builder.energyDiscount;
        this.damageBonus = builder.damageBonus;
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

    /** Potion effects held for as long as the module is installed and the piece is worn. */
    @Nonnull
    public Map<PotionEffectType, Integer> getEffects() {
        return effects;
    }

    @Nonnull
    public Set<SetTrait> getTraits() {
        return traits;
    }

    /** Fraction of ability energy cost waived, 0.0 - 0.9. */
    public double getEnergyDiscount() {
        return energyDiscount;
    }

    /** Additional melee damage multiplier, 0.0 for none. */
    public double getDamageBonus() {
        return damageBonus;
    }

    /** Slimefun item id of the physical module item. */
    @Nonnull
    public String getItemId() {
        return "ARCANE_MODULE_" + id.toUpperCase();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ArmorModule module && module.id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ArmorModule[" + id + "]";
    }

    /** Fluent builder. */
    public static final class Builder {

        private final String id;
        private final String displayName;
        private final Map<PotionEffectType, Integer> effects = new ConcurrentHashMap<>();
        private final Set<SetTrait> traits = EnumSet.noneOf(SetTrait.class);
        private String description = "";
        private Material icon = Material.AMETHYST_SHARD;
        private ChatColor color = ChatColor.AQUA;
        private double energyDiscount;
        private double damageBonus;

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
        public Builder effect(@Nonnull PotionEffectType type, int amplifier) {
            this.effects.put(type, amplifier);
            return this;
        }

        @Nonnull
        public Builder trait(@Nonnull SetTrait... values) {
            Collections.addAll(this.traits, values);
            return this;
        }

        @Nonnull
        public Builder energyDiscount(double fraction) {
            if (fraction < 0.0D || fraction > 0.9D) {
                throw new IllegalArgumentException("energy discount must be within 0.0 - 0.9");
            }
            this.energyDiscount = fraction;
            return this;
        }

        @Nonnull
        public Builder damageBonus(double fraction) {
            this.damageBonus = fraction;
            return this;
        }

        @Nonnull
        public ArmorModule build() {
            return new ArmorModule(this);
        }
    }

    /** Convenience for callers that want the trait list as a plain {@link List}. */
    @Nonnull
    public List<SetTrait> traitList() {
        return List.copyOf(traits);
    }
}
