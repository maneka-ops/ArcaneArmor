package me.arcane.armor.content.ability;

import javax.annotation.Nonnull;
import me.arcane.armor.api.ability.AbilityContext;
import me.arcane.armor.api.ability.AbilityTrigger;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Void ultimate: tears a rift that drags everything nearby toward it, then collapses.
 *
 * <p>The pull is applied as velocity rather than teleportation so players keep control of their
 * own movement and can fight their way out of it.
 */
public final class VoidRiftAbility extends AbstractAbility {

    private static final long PERIOD_TICKS = 10L;

    public VoidRiftAbility() {
        super("void_rift", "Void Rift", "Opens a rift that pulls enemies in, then collapses.",
                AbilityTrigger.ULTIMATE, 680, 55.0D);
    }

    @Override
    public void execute(@Nonnull AbilityContext context) {
        Location center = context.location().clone().add(0, 1, 0);
        double radius = context.scaled(7.0D);
        double collapseDamage = context.scaled(12.0D);
        int pulls = context.scaledInt(4.0D);

        new BukkitRunnable() {
            private int remaining = pulls;

            @Override
            public void run() {
                if (remaining-- <= 0) {
                    collapse(context, center, radius, collapseDamage);
                    cancel();
                    return;
                }

                for (LivingEntity target : nearbyTargets(context, radius)) {
                    Vector pull = center.toVector()
                            .subtract(target.getLocation().toVector())
                            .normalize()
                            .multiply(0.45D);
                    target.setVelocity(target.getVelocity().add(pull));
                }

                context.world().spawnParticle(Particle.PORTAL, center, 120, 1.0D, 1.0D, 1.0D, 1.0D);
                context.world().playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8F, 0.6F);
            }
        }.runTaskTimer(context.plugin(), 0L, PERIOD_TICKS);
    }

    private void collapse(
            @Nonnull AbilityContext context,
            @Nonnull Location center,
            double radius,
            double damage) {

        for (LivingEntity target : nearbyTargets(context, radius)) {
            target.damage(damage, context.player());
        }

        context.world().spawnParticle(Particle.REVERSE_PORTAL, center, 200, 0.6D, 0.6D, 0.6D, 1.2D);
        context.world().playSound(center, Sound.ENTITY_ENDERMAN_DEATH, 1.0F, 0.5F);
    }
}
