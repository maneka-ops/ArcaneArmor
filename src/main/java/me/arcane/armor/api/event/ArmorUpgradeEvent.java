package me.arcane.armor.api.event;

import javax.annotation.Nonnull;
import me.arcane.armor.api.armor.ArmorTier;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Fired at the Upgrade Altar before a piece climbs a tier. Cancelling leaves both the piece and
 * the consumed materials alone.
 */
public class ArmorUpgradeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ItemStack armorPiece;
    private final ArmorTier fromTier;
    private final ArmorTier toTier;
    private boolean cancelled;

    public ArmorUpgradeEvent(
            @Nonnull Player player,
            @Nonnull ItemStack armorPiece,
            @Nonnull ArmorTier fromTier,
            @Nonnull ArmorTier toTier) {
        super(player);
        this.armorPiece = armorPiece;
        this.fromTier = fromTier;
        this.toTier = toTier;
    }

    @Nonnull
    public ItemStack getArmorPiece() {
        return armorPiece;
    }

    @Nonnull
    public ArmorTier getFromTier() {
        return fromTier;
    }

    @Nonnull
    public ArmorTier getToTier() {
        return toTier;
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
