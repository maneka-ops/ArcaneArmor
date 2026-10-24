package me.arcane.armor.api.event;

import javax.annotation.Nonnull;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Fired whenever a piece of armor gains or loses Arcane Energy.
 *
 * <p>Cancelling leaves the stored energy untouched, which is the hook a "creative energy" or
 * "no drain in spawn" policy plugs into.
 */
public class ArcaneEnergyChangeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    /** Why the energy moved. */
    public enum Cause {
        /** An ability was cast. */
        ABILITY,
        /** Passive regeneration from the tick task. */
        REGENERATION,
        /** A Crystal Charger or an energy item topped the piece up. */
        CHARGE,
        /** An admin command or another plugin. */
        EXTERNAL
    }

    private final ItemStack armorPiece;
    private final Cause cause;
    private final int previousEnergy;
    private int newEnergy;
    private boolean cancelled;

    public ArcaneEnergyChangeEvent(
            @Nonnull Player player,
            @Nonnull ItemStack armorPiece,
            @Nonnull Cause cause,
            int previousEnergy,
            int newEnergy) {
        super(player);
        this.armorPiece = armorPiece;
        this.cause = cause;
        this.previousEnergy = previousEnergy;
        this.newEnergy = newEnergy;
    }

    @Nonnull
    public ItemStack getArmorPiece() {
        return armorPiece;
    }

    @Nonnull
    public Cause getCause() {
        return cause;
    }

    public int getPreviousEnergy() {
        return previousEnergy;
    }

    public int getNewEnergy() {
        return newEnergy;
    }

    public void setNewEnergy(int newEnergy) {
        this.newEnergy = Math.max(0, newEnergy);
    }

    public int getDelta() {
        return newEnergy - previousEnergy;
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
