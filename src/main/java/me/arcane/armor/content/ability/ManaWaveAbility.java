package me.arcane.armor.content.ability;

import javax.annotation.Nonnull;
import me.arcane.armor.api.ability.AbilityContext;
import me.arcane.armor.api.ability.AbilityTrigger;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

/**
 * Apprentice active: a ring of force that damages and shoves everything around the caster.
 */
public final class ManaWaveAbility extends AbstractAbility {

    public ManaWaveAbility() {
        super("mana_wave", "Mana Wave", "Knocks nearby enemies back with raw mana.",
                AbilityTrigger.ACTIVE, 120, 8.0D);
    }

    @Override
    public void execute(@Nonnull AbilityContext context) {
        double radius = context.scaled(4.0D);
        double damage = context.scaled(3.0D);

        for (LivingEntity target : nearbyTargets(context, radius)) {
            target.damage(damage, context.player());

            Vector push = target.getLocation().toVector()
                    .subtract(context.location().toVector())
                    .normalize()
                    .multiply(0.9D)
                    .setY(0.35D);
            target.setVelocity(target.getVelocity().add(push));
        }

        context.world().spawnParticle(Particle.ENCHANT, context.location().add(0, 1, 0),
                80, radius / 2.0D, 0.4D, radius / 2.0D, 0.8D);
        context.world().playSound(context.location(), Sound.ENTITY_EVOKER_CAST_SPELL, 1.0F, 1.5F);
    }
}
