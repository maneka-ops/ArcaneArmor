package me.arcane.armor.content.ability;

import javax.annotation.Nonnull;
import me.arcane.armor.api.ability.AbilityContext;
import me.arcane.armor.api.ability.AbilityTrigger;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Infernal ultimate: a large detonation centred on the caster.
 *
 * <p>The explosion is cosmetic only — damage is applied by hand to living targets — so it never
 * breaks terrain, never lights the world on fire and cannot be used to grief through walls.
 */
public final class InfernoExplosionAbility extends AbstractAbility {

    public InfernoExplosionAbility() {
        super("inferno_explosion", "Inferno Explosion", "Erupts in fire, burning everything close by.",
                AbilityTrigger.ULTIMATE, 650, 50.0D);
    }

    @Override
    public void execute(@Nonnull AbilityContext context) {
        double radius = context.scaled(6.5D);
        double damage = context.scaled(11.0D);
        int burnTicks = context.scaledInt(140.0D);

        for (LivingEntity target : nearbyTargets(context, radius)) {
            target.damage(damage, context.player());
            target.setFireTicks(Math.max(target.getFireTicks(), burnTicks));
        }

        context.player().addPotionEffect(
                new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 300, 0, true, true, true));

        context.world().spawnParticle(Particle.EXPLOSION_EMITTER, context.location(), 4);
        context.world().spawnParticle(Particle.FLAME, context.location().add(0, 1, 0),
                220, radius / 2.0D, 1.2D, radius / 2.0D, 0.35D);
        context.world().playSound(context.location(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.7F);
    }
}
