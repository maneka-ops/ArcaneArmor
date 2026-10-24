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
 * Apprentice ultimate: detonates the caster's stored mana, damaging everything nearby and leaving
 * the caster briefly overcharged.
 *
 * <p>Scales with how much energy was actually spent, so casting it on a full set hits harder than
 * casting it the moment the bar allows.
 */
public final class ManaBurstAbility extends AbstractAbility {

    public ManaBurstAbility() {
        super("mana_burst", "Mana Burst", "Detonates stored mana in a wide, heavy blast.",
                AbilityTrigger.ULTIMATE, 600, 45.0D);
    }

    @Override
    public void execute(@Nonnull AbilityContext context) {
        double radius = context.scaled(7.0D);
        double damage = context.scaled(9.0D) + context.energySpent() / 100.0D;

        for (LivingEntity target : nearbyTargets(context, radius)) {
            target.damage(damage, context.player());
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, true, true, true));
        }

        context.player().addPotionEffect(
                new PotionEffect(PotionEffectType.SPEED, 200, 1, true, true, true));
        context.player().addPotionEffect(
                new PotionEffect(PotionEffectType.ABSORPTION, 200, 2, true, true, true));

        context.world().spawnParticle(Particle.EXPLOSION_EMITTER, context.location().add(0, 1, 0), 3);
        context.world().spawnParticle(Particle.WITCH, context.location().add(0, 1, 0),
                200, radius / 2.0D, 1.0D, radius / 2.0D, 1.0D);
        context.world().playSound(context.location(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 1.8F);
    }
}
