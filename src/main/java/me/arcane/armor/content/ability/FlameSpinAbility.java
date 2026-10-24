package me.arcane.armor.content.ability;

import javax.annotation.Nonnull;
import me.arcane.armor.api.ability.AbilityContext;
import me.arcane.armor.api.ability.AbilityTrigger;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

/**
 * Infernal active: a spin that sets everything in reach alight.
 */
public final class FlameSpinAbility extends AbstractAbility {

    public FlameSpinAbility() {
        super("flame_spin", "Flame Spin", "Sweeps a ring of fire around you.",
                AbilityTrigger.ACTIVE, 130, 7.0D);
    }

    @Override
    public void execute(@Nonnull AbilityContext context) {
        double radius = context.scaled(4.5D);
        double damage = context.scaled(4.0D);
        int burnTicks = context.scaledInt(80.0D);

        for (LivingEntity target : nearbyTargets(context, radius)) {
            target.damage(damage, context.player());
            target.setFireTicks(Math.max(target.getFireTicks(), burnTicks));
        }

        for (int step = 0; step < 3; step++) {
            context.world().spawnParticle(Particle.FLAME,
                    context.location().add(0, 0.4D + step * 0.4D, 0),
                    40, radius / 2.0D, 0.1D, radius / 2.0D, 0.02D);
        }
        context.world().playSound(context.location(), Sound.ITEM_FIRECHARGE_USE, 1.0F, 0.8F);
    }
}
