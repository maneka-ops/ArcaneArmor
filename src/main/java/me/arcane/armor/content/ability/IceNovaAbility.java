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
 * Frost active: freezes everything around the caster in place.
 */
public final class IceNovaAbility extends AbstractAbility {

    public IceNovaAbility() {
        super("ice_nova", "Ice Nova", "Freezes nearby enemies solid.",
                AbilityTrigger.ACTIVE, 135, 9.0D);
    }

    @Override
    public void execute(@Nonnull AbilityContext context) {
        double radius = context.scaled(5.0D);
        double damage = context.scaled(3.5D);
        int freezeTicks = context.scaledInt(120.0D);

        for (LivingEntity target : nearbyTargets(context, radius)) {
            target.damage(damage, context.player());
            target.setFreezeTicks(Math.max(target.getFreezeTicks(), freezeTicks));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, freezeTicks, 2, true, true, true));
        }

        context.world().spawnParticle(Particle.SNOWFLAKE, context.location().add(0, 1, 0),
                120, radius / 2.0D, 0.8D, radius / 2.0D, 0.05D);
        context.world().playSound(context.location(), Sound.BLOCK_GLASS_BREAK, 1.0F, 0.6F);
    }
}
