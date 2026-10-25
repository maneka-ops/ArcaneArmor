package me.arcane.armor.gui;

import javax.annotation.Nonnull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/**
 * Guards the read-only menus.
 *
 * <p>Cancelling by holder rather than by slot index means a menu can be relaid out without any
 * risk of leaving a hole an item could be pulled through — the historic way custom GUIs turn into
 * item duplication bugs.
 */
public final class GuiListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(@Nonnull InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof ArcaneMenuHolder holder
                && isReadOnly(holder)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(@Nonnull InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof ArcaneMenuHolder holder
                && isReadOnly(holder)) {
            event.setCancelled(true);
        }
    }

    private static boolean isReadOnly(@Nonnull ArcaneMenuHolder holder) {
        return holder.getType() == ArcaneMenuHolder.Type.ARMOR_INFO
                || holder.getType() == ArcaneMenuHolder.Type.QUESTS
                || holder.getType() == ArcaneMenuHolder.Type.LEADERBOARD;
    }
}
