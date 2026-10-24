package me.arcane.armor.api.event;

import javax.annotation.Nonnull;
import me.arcane.armor.api.module.ArmorModule;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Fired when a module is about to be installed into or removed from a piece of armor.
 */
public class ModuleInstallEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ItemStack armorPiece;
    private final ArmorModule module;
    private final boolean removal;
    private boolean cancelled;

    public ModuleInstallEvent(
            @Nonnull Player player,
            @Nonnull ItemStack armorPiece,
            @Nonnull ArmorModule module,
            boolean removal) {
        super(player);
        this.armorPiece = armorPiece;
        this.module = module;
        this.removal = removal;
    }

    @Nonnull
    public ItemStack getArmorPiece() {
        return armorPiece;
    }

    @Nonnull
    public ArmorModule getModule() {
        return module;
    }

    /** {@code true} when the module is being pulled back out of the piece. */
    public boolean isRemoval() {
        return removal;
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
