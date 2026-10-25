package me.arcane.armor.config;

import javax.annotation.Nonnull;
import me.arcane.armor.api.ability.Ability;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Typed view over {@code config.yml}.
 *
 * <p>Reads go through this class rather than through {@code plugin.getConfig()} so that a reload
 * swaps one field and every caller sees the new values immediately, with no listener re-binding
 * and no stale copies held anywhere.
 */
public final class ArcaneConfig {

    private final Plugin plugin;
    private volatile FileConfiguration config;

    public ArcaneConfig(@Nonnull Plugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    /** Re-reads the file from disk. Safe to call while the server is running. */
    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    @Nonnull
    public FileConfiguration raw() {
        return config;
    }

    // ---- Tasks -------------------------------------------------------------------------------

    /** How often, in ticks, passives are re-applied and armor state is recomputed. */
    public int getTickInterval() {
        return Math.max(10, config.getInt("tasks.tick-interval", 20));
    }

    /** How often, in ticks, worn armor regenerates energy. */
    public int getEnergyRegenInterval() {
        return Math.max(20, config.getInt("tasks.energy-regen-interval", 100));
    }

    /**
     * Whether the first player ever to forge a complete set is announced in chat.
     *
     * <p>The claim is remembered in quest-data.yml, so a restart does not reset who got there
     * first and the broadcast can never repeat for the same set.
     */
    public boolean isWorldFirstBroadcast() {
        return config.getBoolean("achievements.world-first-broadcast", true);
    }

    /**
     * Whether every individual armor piece forge is announced in chat.
     *
     * <p>Each player can forge a given piece only once per set, so these events are naturally
     * rare and useful as public status updates about the quest ladder.
     */
    public boolean isPieceForgedBroadcast() {
        return config.getBoolean("achievements.piece-forged-broadcast", true);
    }

    /**
     * Whether the first player ever to bring a piece of a set to Ascended tier is announced.
     *
     * <p>The claim is remembered in ascension-data.yml regardless of this toggle, so turning the
     * broadcast on later does not replay the milestone for a set someone already maxed.
     */
    public boolean isFirstAscendedBroadcast() {
        return config.getBoolean("achievements.first-ascended-broadcast", true);
    }

    /**
     * Whether Arcane Energy gates abilities at all.
     *
     * <p>Off by default: the armor is a set of effects and protections, and abilities are held in
     * check by their cooldowns rather than by a resource the player has to go and refill. Servers
     * that want energy as a progression sink can turn it back on, and every energy mechanic — the
     * Crystal Charger, the regen task, the placeholders — keeps working when they do.
     */
    public boolean isEnergyEnabled() {
        return config.getBoolean("energy.enabled", false);
    }

    /** Energy restored per regeneration pass, before the sleeping bonus. */
    public int getEnergyRegenAmount() {
        return Math.max(0, config.getInt("energy.regen-amount", 10));
    }

    /** Multiplier applied to regeneration while the player is in bed. */
    public double getSleepRegenMultiplier() {
        return config.getDouble("energy.sleep-multiplier", 8.0D);
    }

    // ---- Abilities ---------------------------------------------------------------------------

    public boolean isAbilityEnabled(@Nonnull Ability ability) {
        return config.getBoolean("abilities." + ability.getId() + ".enabled", true);
    }

    /** Whether abilities fire at all; the /arcane abilities toggle starts from this value. */
    public boolean isAllAbilitiesEnabled() {
        return config.getBoolean("abilities.enabled", true);
    }

    public int getEnergyCost(@Nonnull Ability ability) {
        return Math.max(0, config.getInt("abilities." + ability.getId() + ".energy-cost",
                ability.getDefaultEnergyCost()));
    }

    public double getCooldownSeconds(@Nonnull Ability ability) {
        return Math.max(0.0D, config.getDouble("abilities." + ability.getId() + ".cooldown",
                ability.getDefaultCooldownSeconds()));
    }

    /** Whether abilities check {@code arcane.use} before firing. */
    public boolean isPermissionRequired() {
        return config.getBoolean("abilities.require-permission", false);
    }

    // ---- Feedback ----------------------------------------------------------------------------

    public boolean isActionBarEnabled() {
        return config.getBoolean("feedback.action-bar", true);
    }

    public boolean isParticlesEnabled() {
        return config.getBoolean("feedback.particles", true);
    }

    public boolean isSoundsEnabled() {
        return config.getBoolean("feedback.sounds", true);
    }

    /** Ambient aura around players in a full set; the first thing to switch off on a busy server. */
    public boolean isAuraEnabled() {
        return config.getBoolean("feedback.aura", true);
    }

    // ---- Balance -----------------------------------------------------------------------------

    /** Global multiplier on every set's armor and toughness contribution. */
    public double getDefenseMultiplier() {
        return config.getDouble("balance.defense-multiplier", 1.0D);
    }

    public boolean isUpgradeConsumingMaterials() {
        return config.getBoolean("balance.upgrade-consumes-materials", true);
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("debug", false);
    }
}
