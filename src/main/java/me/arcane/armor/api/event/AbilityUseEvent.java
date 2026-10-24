package me.arcane.armor.api.event;

import javax.annotation.Nonnull;
import me.arcane.armor.api.ability.Ability;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Fired before an ability runs, after its cooldown and energy checks pass but before either is
 * charged. Cancelling refunds nothing because nothing has been spent yet.
 *
 * <p>The energy cost is mutable so other plugins can apply their own discounts or surcharges.
 */
public class AbilityUseEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Ability ability;
    private final ArmorSetDefinition set;
    private int energyCost;
    private boolean cancelled;

    public AbilityUseEvent(
            @Nonnull Player player,
            @Nonnull Ability ability,
            @Nonnull ArmorSetDefinition set,
            int energyCost) {
        super(player);
        this.ability = ability;
        this.set = set;
        this.energyCost = energyCost;
    }

    @Nonnull
    public Ability getAbility() {
        return ability;
    }

    @Nonnull
    public ArmorSetDefinition getSet() {
        return set;
    }

    public int getEnergyCost() {
        return energyCost;
    }

    public void setEnergyCost(int energyCost) {
        this.energyCost = Math.max(0, energyCost);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Nonnull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @Nonnull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
