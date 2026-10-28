package me.arcane.armor.content.ability;

import java.util.List;
import javax.annotation.Nonnull;
import me.arcane.armor.api.ability.AbilityContext;
import me.arcane.armor.api.ability.AbilityTrigger;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Storm ultimate: a rolling storm that strikes a new target every half second.
 *
 * <p>Spreading the strikes over time keeps a single cast from spawning dozens of effects in one
 * tick, and the runnable cancels itself the moment the caster leaves or the storm runs out, so
 * nothing outlives the player. Kept deliberately thin per strike — the ultimate is sustained
 * pressure and area denial, not a one-cast delete button.
 */
public final class ThunderstormAbility extends AbstractAbility {

    private static final long PERIOD_TICKS = 10L;

    public ThunderstormAbility() {
        super("thunderstorm", "Thunderstorm", "A storm follows you, striking nearby enemies.",
                AbilityTrigger.ULTIMATE, 700, 60.0D);
    }

    @Override
    public void execute(@Nonnull AbilityContext context) {
        int strikes = context.scaledInt(4.0D);
        double radius = context.scaled(6.0D);
        double damage = context.scaled(4.0D);

        new BukkitRunnable() {
            private int remaining = strikes;

            @Override
            public void run() {
                if (remaining-- <= 0 || !context.player().isOnline() || context.player().isDead()) {
                    cancel();
                    return;
                }

                List<LivingEntity> targets = nearbyTargets(context, radius);
                if (targets.isEmpty()) {
                    return;
                }

                LivingEntity target = targets.get(0);
                context.world().strikeLightningEffect(target.getLocation());
                target.damage(damage, context.player());
                context.world().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8F, 1.0F);
            }
        }.runTaskTimer(context.plugin(), 0L, PERIOD_TICKS);
    }
}
