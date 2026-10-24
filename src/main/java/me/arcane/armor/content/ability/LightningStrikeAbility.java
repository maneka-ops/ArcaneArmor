package me.arcane.armor.content.ability;

import javax.annotation.Nonnull;
import me.arcane.armor.api.ability.AbilityContext;
import me.arcane.armor.api.ability.AbilityTrigger;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;

/**
 * Storm active: calls lightning down on whatever the caster is looking at.
 *
 * <p>Uses an effect-only strike plus direct damage rather than a real bolt, so it cannot start
 * fires, cannot convert villagers or pigs, and behaves identically in every world.
 */
public final class LightningStrikeAbility extends AbstractAbility {

    private static final double RANGE = 24.0D;

    public LightningStrikeAbility() {
        super("lightning_strike", "Lightning Strike", "Calls a bolt down on your target.",
                AbilityTrigger.ACTIVE, 150, 6.0D);
    }

    @Override
    public boolean canUse(@Nonnull AbilityContext context) {
        return findTarget(context) != null;
    }

    @Override
    public void execute(@Nonnull AbilityContext context) {
        LivingEntity target = findTarget(context);
        if (target == null) {
            return;
        }

        context.world().strikeLightningEffect(target.getLocation());
        target.damage(context.scaled(8.0D), context.player());
        context.world().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0F, 1.2F);
    }

    private LivingEntity findTarget(@Nonnull AbilityContext context) {
        RayTraceResult result = context.world().rayTraceEntities(
                context.player().getEyeLocation(),
                context.player().getEyeLocation().getDirection(),
                RANGE,
                entity -> entity instanceof LivingEntity
                        && !(entity instanceof ArmorStand)
                        && !entity.equals(context.player()));

        if (result == null || !(result.getHitEntity() instanceof LivingEntity living) || living instanceof ArmorStand) {
            return null;
        }
        return living;
    }
}
