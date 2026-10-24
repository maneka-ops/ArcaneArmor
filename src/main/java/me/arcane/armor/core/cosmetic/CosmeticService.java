package me.arcane.armor.core.cosmetic;

import javax.annotation.Nonnull;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.config.ArcaneConfig;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Particles and sounds.
 *
 * <p>Every method is a no-op when the matching toggle is off, so a server that needs the frames
 * back can flatten the cosmetic layer without any behavioural change. Emission is always local to
 * the acting player's world and radius-limited by the client's own particle settings.
 */
public final class CosmeticService {

    private static final int AURA_POINTS = 12;

    private final ArcaneConfig config;

    public CosmeticService(@Nonnull ArcaneConfig config) {
        this.config = config;
    }

    /** Slow ring around a player wearing a full set, drawn once per tick pass. */
    public void aura(@Nonnull Player player, @Nonnull ArmorSetDefinition set) {
        if (!config.isParticlesEnabled() || !config.isAuraEnabled()) {
            return;
        }

        World world = player.getWorld();
        Location center = player.getLocation();
        double radius = 0.8D;
        double phase = (center.getWorld().getGameTime() % 40L) / 40.0D * Math.PI * 2.0D;

        for (int index = 0; index < AURA_POINTS; index++) {
            double angle = phase + (Math.PI * 2.0D * index / AURA_POINTS);
            Location point = center.clone().add(Math.cos(angle) * radius, 0.15D, Math.sin(angle) * radius);
            world.spawnParticle(Particle.DUST, point, 1, 0.0D, 0.0D, 0.0D, 0.0D,
                    new Particle.DustOptions(toBukkitColor(set), 0.9F));
        }
    }

    /** Burst played where an ability is cast. */
    public void castBurst(@Nonnull Player player, @Nonnull ArmorSetDefinition set) {
        if (config.isParticlesEnabled()) {
            player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0),
                    40, 0.5D, 0.8D, 0.5D, 0.6D);
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0),
                    25, 0.5D, 0.8D, 0.5D, 0.0D,
                    new Particle.DustOptions(toBukkitColor(set), 1.2F));
        }

        if (config.isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.7F, 1.4F);
        }
    }

    /** Played when a cast is refused for cooldown or energy. */
    public void denied(@Nonnull Player player) {
        if (config.isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6F, 0.6F);
        }
    }

    /** Played at the Upgrade Altar when a piece climbs a tier. */
    public void upgrade(@Nonnull Player player) {
        if (config.isParticlesEnabled()) {
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0),
                    60, 0.6D, 1.0D, 0.6D, 0.4D);
        }
        if (config.isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0F, 1.6F);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);
        }
    }

    /** Celebration around a player who just forged an armor piece at the Arcane Forge. */
    public void forgeBurst(@Nonnull Player player, @Nonnull ArmorSetDefinition set) {
        if (config.isParticlesEnabled()) {
            player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 1, 0),
                    30, 0.5D, 0.8D, 0.5D, 0.15D);
            player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0),
                    20, 0.4D, 0.8D, 0.4D, 0.05D);
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0),
                    30, 0.5D, 0.8D, 0.5D, 0.0D,
                    new Particle.DustOptions(toBukkitColor(set), 1.4F));
        }
        if (config.isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.3F);
        }
    }

    /** Bigger celebration around the first player ever to forge a complete set. */
    public void worldFirstBurst(@Nonnull Player player, @Nonnull ArmorSetDefinition set) {
        if (config.isParticlesEnabled()) {
            player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 1, 0),
                    100, 0.8D, 1.2D, 0.8D, 0.35D);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0),
                    80, 0.7D, 1.1D, 0.7D, 0.5D);
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0),
                    60, 0.8D, 1.2D, 0.8D, 0.0D,
                    new Particle.DustOptions(toBukkitColor(set), 1.8F));
        }
        if (config.isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.2F);
        }
    }

    /** Horizontal ring, the shape most area abilities telegraph with. */
    public void ring(@Nonnull Location center, @Nonnull Particle particle, double radius, int points) {
        if (!config.isParticlesEnabled()) {
            return;
        }

        World world = center.getWorld();
        for (int index = 0; index < points; index++) {
            double angle = Math.PI * 2.0D * index / points;
            world.spawnParticle(particle,
                    center.clone().add(Math.cos(angle) * radius, 0.2D, Math.sin(angle) * radius),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** Straight line of particles, used by dashes and beams. */
    public void beam(@Nonnull Location from, @Nonnull Location to, @Nonnull Particle particle) {
        if (!config.isParticlesEnabled()) {
            return;
        }

        World world = from.getWorld();
        double distance = from.distance(to);
        int steps = (int) Math.min(60.0D, distance * 4.0D);
        for (int step = 0; step <= steps; step++) {
            double progress = steps == 0 ? 0.0D : (double) step / steps;
            Location point = from.clone().add(to.clone().subtract(from).toVector().multiply(progress));
            world.spawnParticle(particle, point, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    public void sound(@Nonnull Player player, @Nonnull Sound sound, float volume, float pitch) {
        if (config.isSoundsEnabled()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    @Nonnull
    private static Color toBukkitColor(@Nonnull ArmorSetDefinition set) {
        return set.getLeatherColor();
    }
}
