package me.arcane.armor.api.armor;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.arcane.armor.api.registry.Keyed;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;

/**
 * Declarative description of one Arcane armor set.
 *
 * <p>A set is data, not code: it names the passives, traits and abilities it grants, and the
 * services read that description at runtime. Registering a tenth set is therefore a matter of
 * building another definition, not of writing another listener.
 */
public final class ArmorSetDefinition implements Keyed {

    private final String id;
    private final String displayName;
    private final ChatColor color;
    private final Color leatherColor;
    private final Material themeMaterial;
    private final List<PassiveEffect> passives;
    private final Set<SetTrait> traits;
    private final String activeAbilityId;
    private final String ultimateAbilityId;
    private final int baseEnergyCapacity;
    private final double armorPerPiece;
    private final double toughnessPerPiece;
    private final ArmorTier startingTier;
    private final String previousSetId;
    private final int enchantBase;
    private final SetTrait signatureTrait;
    private final ArmorTier scalingCeiling;

    private ArmorSetDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.color = builder.color;
        this.leatherColor = builder.leatherColor;
        this.themeMaterial = builder.themeMaterial;
        this.passives = List.copyOf(builder.passives);
        this.traits = builder.traits.isEmpty()
                ? Collections.unmodifiableSet(EnumSet.noneOf(SetTrait.class))
                : Collections.unmodifiableSet(EnumSet.copyOf(builder.traits));
        this.activeAbilityId = builder.activeAbilityId;
        this.ultimateAbilityId = builder.ultimateAbilityId;
        this.baseEnergyCapacity = builder.baseEnergyCapacity;
        this.armorPerPiece = builder.armorPerPiece;
        this.toughnessPerPiece = builder.toughnessPerPiece;
        this.startingTier = builder.startingTier;
        this.previousSetId = builder.previousSetId;
        this.enchantBase = builder.enchantBase;
        this.signatureTrait = builder.signatureTrait;
        this.scalingCeiling = builder.scalingCeiling;
    }

    /**
     * Enchantment level this set starts from, before the tier is added on top.
     *
     * <p>This is the dial that separates an early set from a late one. A set with a base of 2 reads
     * as Protection II at tier one and climbs from there; a base of 10 is already at the cap, which
     * is what an endgame set is for.
     */
    public int getEnchantBase() {
        return enchantBase;
    }

    /**
     * Id of the set that has to be crafted before this one, or {@code null} for an entry set.
     *
     * <p>The chain is what makes the ladder a ladder: a set's recipe consumes the matching piece of
     * the set below it, so reaching the last armor means having built every set before it.
     */
    @Nullable
    public String getPreviousSetId() {
        return previousSetId;
    }

    /**
     * The tier this set's numbers stop growing at, or {@code null} when the whole ladder scales
     * freely.
     *
     * <p>A capped set still climbs every rung — extra module slots, rune slots, and the preview
     * panels all acknowledge the higher tiers — but its stats, energy capacity, cooldowns and
     * enchantment levels read from {@link #effectiveTier(ArmorTier)}, which refuses to rise past
     * the ceiling. This is how one set can be strong at its peak without also being the strongest
     * thing in the game at max tier.
     */
    @Nullable
    public ArmorTier getScalingCeiling() {
        return scalingCeiling;
    }

    /**
     * The tier whose multiplier sheet actually governs this set at the given tier.
     *
     * <p>Beyond the ceiling the set keeps climbing the ladder (slots, lore, milestones) but scales
     * from the ceiling's sheet, so an Ascended piece of a capped set performs like a Tier V one.
     */
    @Nonnull
    public ArmorTier effectiveTier(@Nonnull ArmorTier tier) {
        if (scalingCeiling == null || tier.ordinal() <= scalingCeiling.ordinal()) {
            return tier;
        }
        return scalingCeiling;
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
    public ChatColor getColor() {
        return color;
    }

    @Nonnull
    public Color getLeatherColor() {
        return leatherColor;
    }

    /** Material shown in the guide and used as the crafting theme ingredient. */
    @Nonnull
    public Material getThemeMaterial() {
        return themeMaterial;
    }

    @Nonnull
    public List<PassiveEffect> getPassives() {
        return passives;
    }

    @Nonnull
    public Set<SetTrait> getTraits() {
        return traits;
    }

    /**
     * The behavior unlocked when every worn piece of this set has reached Tier V, or {@code null}
     * when the set has no signature. Granted in addition to the base traits, never instead of them.
     */
    @Nullable
    public SetTrait getSignatureTrait() {
        return signatureTrait;
    }

    /** Ability bound to the active key, available from a single piece upward. */
    @Nullable
    public String getActiveAbilityId() {
        return activeAbilityId;
    }

    /** Ability bound to the ultimate key, gated behind the full set. */
    @Nullable
    public String getUltimateAbilityId() {
        return ultimateAbilityId;
    }

    public int getBaseEnergyCapacity() {
        return baseEnergyCapacity;
    }

    public double getArmorPerPiece() {
        return armorPerPiece;
    }

    public double getToughnessPerPiece() {
        return toughnessPerPiece;
    }

    @Nonnull
    public ArmorTier getStartingTier() {
        return startingTier;
    }

    /** Slimefun item id for one piece of this set, e.g. {@code ARCANE_STORM_HELMET}. */
    @Nonnull
    public String getItemId(@Nonnull ArmorSlot slot) {
        return "ARCANE_" + id.toUpperCase() + "_" + slot.name();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ArmorSetDefinition set && set.id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ArmorSetDefinition[" + id + "]";
    }

    /** Fluent builder; every set is assembled through this. */
    public static final class Builder {

        private final String id;
        private final String displayName;
        private final List<PassiveEffect> passives = new java.util.ArrayList<>();
        private final Set<SetTrait> traits = EnumSet.noneOf(SetTrait.class);
        private ChatColor color = ChatColor.LIGHT_PURPLE;
        private Color leatherColor = Color.PURPLE;
        private Material themeMaterial = Material.AMETHYST_SHARD;
        private String activeAbilityId;
        private String ultimateAbilityId;
        private int baseEnergyCapacity = 1_000;
        private double armorPerPiece = 3.0D;
        private double toughnessPerPiece = 1.0D;
        private ArmorTier startingTier = ArmorTier.TIER_1;
        private String previousSetId;
        private int enchantBase = 3;
        private SetTrait signatureTrait;
        private ArmorTier scalingCeiling;

        private Builder(String id, String displayName) {
            this.id = Objects.requireNonNull(id, "id").toLowerCase();
            this.displayName = Objects.requireNonNull(displayName, "displayName");
        }

        /**
         * Sets the trait unlocked at Tier V, once every worn piece of the set has reached it. The
         * signature is what gives a maxed set a second identity beyond bigger numbers; leave unset
         * for a set with no tier-gated behavior.
         */
        @Nonnull
        public Builder signature(@Nullable SetTrait trait) {
            this.signatureTrait = trait;
            return this;
        }

        @Nonnull
        public Builder color(@Nonnull ChatColor color, @Nonnull Color leatherColor) {
            this.color = color;
            this.leatherColor = leatherColor;
            return this;
        }

        @Nonnull
        public Builder theme(@Nonnull Material themeMaterial) {
            this.themeMaterial = themeMaterial;
            return this;
        }

        /**
         * Sets the enchantment floor for this set; the tier is added to it and the result is capped
         * at ten. Defaults to 3.
         */
        @Nonnull
        public Builder enchantBase(int enchantBase) {
            this.enchantBase = Math.max(0, enchantBase);
            return this;
        }

        /**
         * Places this set one rung above another: its recipes consume the matching piece of that
         * set. Leave unset for an entry set that is craftable from raw materials.
         */
        @Nonnull
        public Builder upgradeFrom(@Nonnull String previousSetId) {
            this.previousSetId = previousSetId.toLowerCase();
            return this;
        }

        @Nonnull
        public Builder passive(@Nonnull PassiveEffect effect) {
            this.passives.add(effect);
            return this;
        }

        @Nonnull
        public Builder trait(@Nonnull SetTrait... values) {
            Collections.addAll(this.traits, values);
            return this;
        }

        @Nonnull
        public Builder active(@Nullable String abilityId) {
            this.activeAbilityId = abilityId;
            return this;
        }

        @Nonnull
        public Builder ultimate(@Nullable String abilityId) {
            this.ultimateAbilityId = abilityId;
            return this;
        }

        @Nonnull
        public Builder energy(int baseEnergyCapacity) {
            this.baseEnergyCapacity = baseEnergyCapacity;
            return this;
        }

        @Nonnull
        public Builder defense(double armorPerPiece, double toughnessPerPiece) {
            this.armorPerPiece = armorPerPiece;
            this.toughnessPerPiece = toughnessPerPiece;
            return this;
        }

        @Nonnull
        public Builder startingTier(@Nonnull ArmorTier tier) {
            this.startingTier = tier;
            return this;
        }

        /**
         * Caps this set's scaling sheet at the given tier: pieces above it keep climbing the ladder
         * (slots, lore, milestones) but keep the ceiling's stats, energy, cooldowns and enchantment
         * levels. Leave unset for a set whose whole ladder scales freely.
         */
        @Nonnull
        public Builder scalingCeiling(@Nullable ArmorTier ceiling) {
            this.scalingCeiling = ceiling;
            return this;
        }

        @Nonnull
        public ArmorSetDefinition build() {
            return new ArmorSetDefinition(this);
        }
    }
}
