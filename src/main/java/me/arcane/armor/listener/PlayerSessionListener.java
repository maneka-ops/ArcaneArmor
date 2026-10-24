package me.arcane.armor.listener;

import javax.annotation.Nonnull;
import me.arcane.armor.core.armor.EquipmentService;
import me.arcane.armor.core.cooldown.CooldownService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Keeps the per-player caches bounded and restricts equipping locked armor.
 *
 * <p>Both services key on UUID, so without this the maps would grow for the lifetime of the server
 * on any world with churn. Joining seeds the cache immediately so a player's first swing is not
 * handled with an empty state.
 */
public final class PlayerSessionListener implements Listener {

    private final EquipmentService equipmentService;
    private final CooldownService cooldownService;

    public PlayerSessionListener(
            @Nonnull EquipmentService equipmentService,
            @Nonnull CooldownService cooldownService) {
        this.equipmentService = equipmentService;
        this.cooldownService = cooldownService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@Nonnull PlayerJoinEvent event) {
        equipmentService.refresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@Nonnull PlayerQuitEvent event) {
        equipmentService.invalidate(event.getPlayer().getUniqueId());
        cooldownService.clear(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(@Nonnull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) {
            return;
        }
        Player player = event.getPlayer();
        if (!equipmentService.canEquip(player, item)) {
            event.setCancelled(true);
            equipmentService.notifyLocked(player, item);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(@Nonnull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            ItemStack toEquip;
            if (event.getClick() == ClickType.NUMBER_KEY) {
                int hotbarSlot = event.getHotbarButton();
                toEquip = (hotbarSlot >= 0 && hotbarSlot < 9) ? player.getInventory().getItem(hotbarSlot) : null;
            } else {
                toEquip = event.getCursor();
            }

            if (toEquip != null && !toEquip.getType().isAir() && !equipmentService.canEquip(player, toEquip)) {
                event.setCancelled(true);
                equipmentService.notifyLocked(player, toEquip);
            }
        } else if (event.isShiftClick() && event.getClickedInventory() == player.getInventory()
                && event.getView().getTopInventory().getType() == InventoryType.CRAFTING) {
            ItemStack current = event.getCurrentItem();
            if (current != null && !current.getType().isAir() && !equipmentService.canEquip(player, current)) {
                event.setCancelled(true);
                equipmentService.notifyLocked(player, current);
            }
        }
    }
}
