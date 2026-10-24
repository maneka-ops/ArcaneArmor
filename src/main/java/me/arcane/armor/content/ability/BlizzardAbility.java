package me.arcane.armor.content.ability;

import javax.annotation.Nonnull;
import me.arcane.armor.api.ability.AbilityContext;
import me.arcane.armor.api.ability.AbilityTrigger;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Frost ultimate: a standing blizzard that keeps chilling whatever walks into it.
 *
 * <p>Ticks a fixed number of times from a fixed centre rather than following the caster, which
 * makes it an area to fight around instead of a second aura.
 */
public final class BlizzardAbility extends AbstractAbility {

    private static final long PERIOD_TICKS = 20L;

    public BlizzardAbility() {
        super("blizzard", "Blizzard", "Leaves a freezing storm on the ground.",
                AbilityTrigger.ULTIMATE, 640, 50.0D);
    }

    @Override
    public void execute(@Nonnull AbilityContext context) {
        int pulses = context.scaledInt(6.0D);
        double radius = context.scaled(6.0D);
        double damage = context.scaled(3.0D);

        new BukkitRunnable() {
            private int remaining = pulses;

            @Override
            public void run() {
                if (remaining-- <= 0 || !context.player().isOnline()) {
                    cancel();
                    return;
                }

                for (LivingEntity target : nearbyTargets(context, radius)) {
                    target.damage(damage, context.player());
                    target.setFreezeTicks(Math.max(target.getFreezeTicks(), 100));
                    target.addPotionEffect(
                            new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, true, true, true));
                }

                context.world().spawnParticle(Particle.SNOWFLAKE, context.location().add(0, 1.5D, 0),
                        150, radius / 2.0D, 1.5D, radius / 2.0D, 0.03D);
                context.world().playSound(context.location(), Sound.WEATHER_RAIN, 0.6F, 0.5F);
            }
        }.runTaskTimer(context.plugin(), 0L, PERIOD_TICKS);
    }
}
