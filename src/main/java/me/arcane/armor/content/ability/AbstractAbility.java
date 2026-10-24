package me.arcane.armor.content.ability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nonnull;
import me.arcane.armor.api.ability.Ability;
import me.arcane.armor.api.ability.AbilityContext;
import me.arcane.armor.api.ability.AbilityTrigger;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Shared plumbing for the bundled abilities: identity, defaults, and the target selection every
 * area ability needs.
 *
 * <p>Concrete abilities are left with nothing but their effect, which is the point — a new ability
 * should be a dozen lines, not a new subsystem.
 */
public abstract class AbstractAbility implements Ability {

    private final String id;
    private final String displayName;
    private final String description;
    private final AbilityTrigger trigger;
    private final int energyCost;
    private final double cooldownSeconds;

    protected AbstractAbility(
            @Nonnull String id,
            @Nonnull String displayName,
            @Nonnull String description,
            @Nonnull AbilityTrigger trigger,
            int energyCost,
            double cooldownSeconds) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.trigger = trigger;
        this.energyCost = energyCost;
        this.cooldownSeconds = cooldownSeconds;
    }

    @Nonnull
    @Override
    public final String getId() {
        return id;
    }

    @Nonnull
    @Override
    public final String getDisplayName() {
        return displayName;
    }

    @Nonnull
    @Override
    public final String getDescription() {
        return description;
    }

    @Nonnull
    @Override
    public final AbilityTrigger getTrigger() {
        return trigger;
    }

    @Override
    public final int getDefaultEnergyCost() {
        return energyCost;
    }

    @Override
    public final double getDefaultCooldownSeconds() {
        return cooldownSeconds;
    }

    /**
     * Living entities within {@code radius} of the caster, excluding the caster and anyone the
     * caster is not allowed to hit.
     *
     * <p>Sorted nearest-first so abilities that only affect the first few targets pick the ones the
     * player would expect.
     */
    @Nonnull
    protected List<LivingEntity> nearbyTargets(@Nonnull AbilityContext context, double radius) {
        Location center = context.location();
        List<LivingEntity> targets = new ArrayList<>();

        for (Entity entity : context.world().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || living instanceof ArmorStand || entity.equals(context.player())) {
                continue;
            }
            if (living instanceof Player other && !canHit(context.player(), other)) {
                continue;
            }
            targets.add(living);
        }

        targets.sort(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(center)));
        return targets;
    }

    /**
     * Whether a player may be hit by another player's ability.
     *
     * <p>Defers to the world's PvP flag, which is the cheapest correct answer without pulling in a
     * region plugin; a server running one can veto any cast through {@code AbilityUseEvent}.
     */
    protected boolean canHit(@Nonnull Player caster, @Nonnull Player target) {
        return target.getWorld().getPVP() && !target.equals(caster);
    }
}
