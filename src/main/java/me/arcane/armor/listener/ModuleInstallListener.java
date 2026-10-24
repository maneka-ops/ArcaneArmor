package me.arcane.armor.listener;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.api.armor.ArmorSlot;
import me.arcane.armor.api.event.ModuleInstallEvent;
import me.arcane.armor.api.module.ArmorModule;
import me.arcane.armor.config.ArcaneConfig;
import me.arcane.armor.config.Messages;
import me.arcane.armor.content.armor.ArmorItemFactory;
import me.arcane.armor.core.armor.EquipmentService;
import me.arcane.armor.core.data.ArmorData;
import me.arcane.armor.core.data.ArmorDataService;
import me.arcane.armor.registry.ArcaneRegistries;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Installs and removes modules by dragging one onto a piece of armor in the inventory.
 *
 * <p>Done as an inventory interaction rather than a machine because installing is meant to be
 * frequent — the whole point of modules is reconfiguring between a mining trip and a fight, and
 * that stops happening if it costs a walk back to a block.
 *
 * <p>Carrying a module in the cursor and left-clicking a piece installs it; shift-right-clicking a
 * piece with an empty cursor pops the last module back out.
 */
public final class ModuleInstallListener implements Listener {

    private final ArcaneRegistries registries;
    private final ArmorDataService dataService;
    private final EquipmentService equipmentService;
    private final ArmorItemFactory itemFactory;
    private final ArcaneConfig config;
    private final Messages messages;

    public ModuleInstallListener(
            @Nonnull ArcaneRegistries registries,
            @Nonnull ArmorDataService dataService,
            @Nonnull EquipmentService equipmentService,
            @Nonnull ArmorItemFactory itemFactory,
            @Nonnull ArcaneConfig config,
            @Nonnull Messages messages) {
        this.registries = registries;
        this.dataService = dataService;
        this.equipmentService = equipmentService;
        this.itemFactory = itemFactory;
        this.config = config;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onClick(@Nonnull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        ArmorData data = dataService.read(clicked).orElse(null);
        if (data == null) {
            return;
        }

        ArmorSetDefinition set = registries.sets().get(data.setId());
        ArmorSlot slot = slotOf(clicked);
        if (set == null || slot == null) {
            return;
        }

        ItemStack cursor = event.getCursor();
        ArmorModule module = resolveModule(cursor);

        if (module != null) {
            event.setCancelled(true);
            install(event, player, clicked, cursor, set, slot, data, module);
            return;
        }

        boolean emptyCursor = cursor.getType().isAir();
        if (emptyCursor && event.getClick() == ClickType.SHIFT_RIGHT && !data.moduleIds().isEmpty()) {
            event.setCancelled(true);
            uninstall(player, clicked, set, slot, data);
        }
    }

    private void install(
            @Nonnull InventoryClickEvent event,
            @Nonnull Player player,
            @Nonnull ItemStack armor,
            @Nonnull ItemStack cursor,
            @Nonnull ArmorSetDefinition set,
            @Nonnull ArmorSlot slot,
            @Nonnull ArmorData data,
            @Nonnull ArmorModule module) {

        if (!data.hasFreeModuleSlot()) {
            messages.send(player, "no-module-slots",
                    "&cNo free module slots. &7({used}/{max}) Upgrade the piece for more.",
                    Map.of("used", String.valueOf(data.moduleIds().size()),
                            "max", String.valueOf(data.tier().getModuleSlots())));
            return;
        }

        if (data.moduleIds().contains(module.getId())) {
            messages.send(player, "module-already-installed", "&cThat module is already installed.");
            return;
        }

        ModuleInstallEvent installEvent = new ModuleInstallEvent(player, armor, module, false);
        Bukkit.getPluginManager().callEvent(installEvent);
        if (installEvent.isCancelled()) {
            return;
        }

        List<String> updated = new ArrayList<>(data.moduleIds());
        updated.add(module.getId());

        ArmorData newData = data.withModules(updated);
        dataService.write(armor, newData);
        itemFactory.refreshPresentation(armor, set, slot, newData, config.getDefenseMultiplier());

        cursor.setAmount(cursor.getAmount() - 1);
        event.getView().setCursor(cursor.getAmount() <= 0 ? null : cursor);

        equipmentService.refresh(player);
        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1.0F, 1.4F);
        messages.send(player, "module-installed", "&aInstalled {module}.",
                Map.of("module", module.getDisplayName()));
    }

    private void uninstall(
            @Nonnull Player player,
            @Nonnull ItemStack armor,
            @Nonnull ArmorSetDefinition set,
            @Nonnull ArmorSlot slot,
            @Nonnull ArmorData data) {

        List<String> remaining = new ArrayList<>(data.moduleIds());
        String removedId = remaining.remove(remaining.size() - 1);

        ArmorModule module = registries.modules().get(removedId);
        if (module == null) {
            // The module no longer exists in this build; drop the id rather than stranding the slot.
            dataService.write(armor, data.withModules(remaining));
            return;
        }

        ModuleInstallEvent removeEvent = new ModuleInstallEvent(player, armor, module, true);
        Bukkit.getPluginManager().callEvent(removeEvent);
        if (removeEvent.isCancelled()) {
            return;
        }

        ArmorData newData = data.withModules(remaining);
        dataService.write(armor, newData);
        itemFactory.refreshPresentation(armor, set, slot, newData, config.getDefenseMultiplier());

        SlimefunItem moduleItem = SlimefunItem.getById(module.getItemId());
        if (moduleItem != null) {
            // Anything that will not fit goes to the floor rather than vanishing.
            player.getInventory().addItem(moduleItem.getItem().clone())
                    .values()
                    .forEach(overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));
        }

        equipmentService.refresh(player);
        player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1.0F, 1.2F);
        messages.send(player, "module-removed", "&eRemoved {module}.",
                Map.of("module", module.getDisplayName()));
    }

    @Nullable
    private ArmorModule resolveModule(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }

        SlimefunItem slimefunItem = SlimefunItem.getByItem(item);
        if (slimefunItem == null) {
            return null;
        }

        for (ArmorModule module : registries.modules().values()) {
            if (module.getItemId().equals(slimefunItem.getId())) {
                return module;
            }
        }
        return null;
    }

    @Nullable
    private static ArmorSlot slotOf(@Nonnull ItemStack item) {
        String type = item.getType().name();
        if (type.endsWith("_HELMET")) {
            return ArmorSlot.HELMET;
        }
        if (type.endsWith("_CHESTPLATE")) {
            return ArmorSlot.CHESTPLATE;
        }
        if (type.endsWith("_LEGGINGS")) {
            return ArmorSlot.LEGGINGS;
        }
        if (type.endsWith("_BOOTS")) {
            return ArmorSlot.BOOTS;
        }
        return null;
    }
}
