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
 * Guardian active: a dome of resistance and absorption over the caster and nearby allies.
 */
public final class GuardianShieldAbility extends AbstractAbility {

    public GuardianShieldAbility() {
        super("guardian_shield", "Guardian Shield", "Shields you and nearby allies from harm.",
                AbilityTrigger.ACTIVE, 160, 20.0D);
    }

    @Override
    public void execute(@Nonnull AbilityContext context) {
        double radius = context.scaled(5.0D);
        int duration = context.scaledInt(140.0D);
        int amplifier = Math.min(2, context.scaledInt(1.0D));

        shield(context.player(), duration, amplifier);

        for (Entity entity : context.world().getNearbyEntities(context.location(), radius, radius, radius)) {
            if (entity instanceof Player ally && !ally.equals(context.player())) {
                shield(ally, duration, amplifier);
            }
        }

        context.world().spawnParticle(Particle.END_ROD, context.location().add(0, 1, 0),
                80, radius / 2.0D, 1.0D, radius / 2.0D, 0.02D);
        context.world().playSound(context.location(), Sound.ITEM_SHIELD_BLOCK, 1.0F, 0.8F);
    }

    private static void shield(@Nonnull Player player, int duration, int amplifier) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, amplifier, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration, amplifier, true, true, true));
    }
}
