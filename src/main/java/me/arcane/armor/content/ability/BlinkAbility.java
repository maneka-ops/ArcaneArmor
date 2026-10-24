package me.arcane.armor.content.ability;

import javax.annotation.Nonnull;
import me.arcane.armor.api.ability.AbilityContext;
import me.arcane.armor.api.ability.AbilityTrigger;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.util.RayTraceResult;

/**
 * Void active: a short teleport along the line of sight.
 *
 * <p>The landing spot is ray-traced against blocks and then pulled back to the last free space, so
 * the ability can never drop a player inside terrain or through a wall into a claim.
 */
public final class BlinkAbility extends AbstractAbility {

    public BlinkAbility() {
        super("blink", "Blink", "Teleports you along your line of sight.",
                AbilityTrigger.ACTIVE, 100, 5.0D);
    }

    @Override
    public void execute(@Nonnull AbilityContext context) {
        double range = context.scaled(8.0D);
        Location eye = context.player().getEyeLocation();

        RayTraceResult hit = context.world().rayTraceBlocks(eye, eye.getDirection(), range);
        Location destination = hit == null
                ? eye.clone().add(eye.getDirection().multiply(range))
                : hit.getHitPosition().toLocation(context.world())
                        .subtract(eye.getDirection().multiply(0.6D));

        destination.setYaw(eye.getYaw());
        destination.setPitch(eye.getPitch());

        // Keep the feet clear: step down until the block below is solid ground or we run out.
        while (destination.getY() > context.world().getMinHeight()
                && destination.getBlock().getType().isAir()
                && destination.clone().subtract(0, 1, 0).getBlock().getType().isAir()) {
            destination.subtract(0, 1, 0);
        }

        Location origin = context.location();
        context.player().teleport(destination);

        context.world().spawnParticle(Particle.PORTAL, origin.add(0, 1, 0), 60, 0.4D, 0.8D, 0.4D, 0.6D);
        context.world().spawnParticle(Particle.PORTAL, destination.clone().add(0, 1, 0),
                60, 0.4D, 0.8D, 0.4D, 0.6D);
        context.world().playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.4F);
    }
}
