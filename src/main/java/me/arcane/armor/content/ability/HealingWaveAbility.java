package me.arcane.armor.content.ability;

import javax.annotation.Nonnull;
import me.arcane.armor.api.ability.AbilityContext;
import me.arcane.armor.api.ability.AbilityTrigger;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Nature active: heals the caster and every nearby player.
 */
public final class HealingWaveAbility extends AbstractAbility {

    public HealingWaveAbility() {
        super("healing_wave", "Healing Wave", "Heals you and nearby allies.",
                AbilityTrigger.ACTIVE, 140, 12.0D);
    }

    @Override
    public void execute(@Nonnull AbilityContext context) {
        double radius = context.scaled(6.0D);
        int amplifier = Math.min(3, context.scaledInt(1.0D));

        context.player().addPotionEffect(
                new PotionEffect(PotionEffectType.REGENERATION, 120, amplifier, true, true, true));

        for (Entity entity : context.world().getNearbyEntities(context.location(), radius, radius, radius)) {
            if (entity instanceof Player ally && !ally.equals(context.player())) {
                ally.addPotionEffect(
                        new PotionEffect(PotionEffectType.REGENERATION, 100, amplifier, true, true, true));
            }
        }

        context.world().spawnParticle(Particle.HAPPY_VILLAGER, context.location().add(0, 1, 0),
                60, radius / 2.0D, 1.0D, radius / 2.0D, 0.1D);
        context.world().playSound(context.location(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
    }
}
