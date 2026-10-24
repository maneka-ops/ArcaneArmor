package me.arcane.armor.content.ability;

import javax.annotation.Nonnull;
import me.arcane.armor.api.ability.AbilityContext;
import me.arcane.armor.api.ability.AbilityTrigger;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

/**
 * Celestial ultimate: drops a meteor on the aimed point after a short, visible fall.
 *
 * <p>The delay is deliberate — the falling trail is the tell that lets a target step out of it,
 * which is what keeps a high-damage ultimate from being an unavoidable execute.
 */
public final class MeteorAbility extends AbstractAbility {

    private static final double AIM_RANGE = 30.0D;
    private static final double FALL_HEIGHT = 18.0D;
    private static final long STEP_TICKS = 2L;

    public MeteorAbility() {
        super("meteor", "Meteor Rain", "Calls a meteor down onto the point you aim at.",
                AbilityTrigger.ULTIMATE, 720, 55.0D);
    }

    @Override
    public void execute(@Nonnull AbilityContext context) {
        Location eye = context.player().getEyeLocation();
        RayTraceResult hit = context.world().rayTraceBlocks(eye, eye.getDirection(), AIM_RANGE);
        Location impact = hit == null
                ? eye.clone().add(eye.getDirection().multiply(AIM_RANGE))
                : hit.getHitPosition().toLocation(context.world());

        double radius = context.scaled(5.0D);
        double damage = context.scaled(14.0D);
        Location start = impact.clone().add(0, FALL_HEIGHT, 0);

        new BukkitRunnable() {
            private double travelled;

            @Override
            public void run() {
                travelled += 2.0D;
                Location point = start.clone().subtract(0, travelled, 0);

                context.world().spawnParticle(Particle.FLAME, point, 30, 0.3D, 0.3D, 0.3D, 0.05D);
                context.world().spawnParticle(Particle.LARGE_SMOKE, point, 10, 0.3D, 0.3D, 0.3D, 0.01D);

                if (travelled < FALL_HEIGHT) {
                    return;
                }

                cancel();
                impactAt(context, impact, radius, damage);
            }
        }.runTaskTimer(context.plugin(), 0L, STEP_TICKS);
    }

    private void impactAt(
            @Nonnull AbilityContext context,
            @Nonnull Location impact,
            double radius,
            double damage) {

        for (Entity entity : context.world().getNearbyEntities(impact, radius, radius, radius)) {
            if (entity instanceof LivingEntity living && !(living instanceof ArmorStand) && !living.equals(context.player())) {
                living.damage(damage, context.player());
                living.setFireTicks(Math.max(living.getFireTicks(), 100));
            }
        }

        context.world().spawnParticle(Particle.EXPLOSION_EMITTER, impact, 3);
        context.world().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.6F);
    }
}
