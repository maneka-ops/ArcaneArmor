package me.arcane.armor.gui;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import me.arcane.armor.api.ability.Ability;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.api.module.ArmorModule;
import me.arcane.armor.api.registry.Registry;
import me.arcane.armor.api.rune.Rune;
import me.arcane.armor.core.ability.AbilityService;
import me.arcane.armor.core.armor.ArmorState;
import me.arcane.armor.core.armor.EquipmentService;
import me.arcane.armor.core.cooldown.CooldownService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Read-only summary of what the player is currently wearing: set, tier, energy, modules, runes and
 * ability readiness.
 *
 * <p>Built fresh on open rather than kept in sync, because the underlying state changes every tick
 * and a stale menu would be worse than no menu.
 */
public final class ArmorInfoGui {

    private static final int SIZE = 45;
    private static final int ENERGY_SLOT = 13;
    private static final int SET_SLOT = 4;
    private static final int MODULE_START = 19;
    private static final int RUNE_START = 28;
    private static final int ACTIVE_SLOT = 38;
    private static final int ULTIMATE_SLOT = 42;

    private final EquipmentService equipmentService;
    private final AbilityService abilityService;
    private final CooldownService cooldownService;
    private final Registry<Ability> abilities;

    public ArmorInfoGui(
            @Nonnull EquipmentService equipmentService,
            @Nonnull AbilityService abilityService,
            @Nonnull CooldownService cooldownService,
            @Nonnull Registry<Ability> abilities) {
        this.equipmentService = equipmentService;
        this.abilityService = abilityService;
        this.cooldownService = cooldownService;
        this.abilities = abilities;
    }

    public void open(@Nonnull Player player) {
        ArmorState state = equipmentService.refresh(player);

        ArcaneMenuHolder holder = new ArcaneMenuHolder(ArcaneMenuHolder.Type.ARMOR_INFO, null);
        Inventory inventory = Bukkit.createInventory(holder, SIZE,
                net.kyori.adventure.text.Component.text("Arcane Armor"));
        holder.setInventory(inventory);

        fillBackground(inventory);

        ArmorSetDefinition set = state.set();
        if (set == null) {
            inventory.setItem(SET_SLOT, icon(Material.BARRIER, ChatColor.RED + "No Arcane armor worn",
                    List.of(ChatColor.GRAY + "Equip a set to see its status here.")));
            player.openInventory(inventory);
            return;
        }

        inventory.setItem(SET_SLOT, icon(set.getThemeMaterial(),
                set.getColor() + set.getDisplayName() + " Set",
                List.of(
                        ChatColor.GRAY + "Pieces worn: " + ChatColor.WHITE + state.piecesWorn() + "/4",
                        ChatColor.GRAY + "Tier: " + state.tier().getColor() + state.tier().getLabel(),
                        ChatColor.GRAY + "Full set bonus: "
                                + (state.isFullSet() ? ChatColor.GREEN + "active" : ChatColor.RED + "inactive"))));

        inventory.setItem(ENERGY_SLOT, icon(Material.AMETHYST_CLUSTER,
                ChatColor.AQUA + "Arcane Energy",
                List.of(
                        ChatColor.WHITE + String.valueOf(state.energy())
                                + ChatColor.DARK_GRAY + " / " + ChatColor.WHITE + state.capacity(),
                        "",
                        energyBar(state))));

        placeModules(inventory, state);
        placeRunes(inventory, state);
        placeAbility(inventory, player, state, ACTIVE_SLOT, set.getActiveAbilityId(), "Active");
        placeAbility(inventory, player, state, ULTIMATE_SLOT, set.getUltimateAbilityId(), "Ultimate");

        player.openInventory(inventory);
    }

    private void placeModules(@Nonnull Inventory inventory, @Nonnull ArmorState state) {
        List<ArmorModule> modules = state.modules();
        for (int index = 0; index < 7; index++) {
            int slot = MODULE_START + index;
            if (index < modules.size()) {
                ArmorModule module = modules.get(index);
                inventory.setItem(slot, icon(module.getIcon(),
                        module.getColor() + module.getDisplayName(),
                        List.of(ChatColor.GRAY + module.getDescription())));
            } else {
                inventory.setItem(slot, icon(Material.GRAY_DYE,
                        ChatColor.DARK_GRAY + "Empty module slot",
                        List.of(ChatColor.GRAY + "Install modules at the Arcane Forge.")));
            }
        }
    }

    private void placeRunes(@Nonnull Inventory inventory, @Nonnull ArmorState state) {
        List<Rune> runes = state.runes();
        for (int index = 0; index < 7; index++) {
            int slot = RUNE_START + index;
            if (index < runes.size()) {
                Rune rune = runes.get(index);
                inventory.setItem(slot, icon(rune.getIcon(),
                        rune.getColor() + rune.getDisplayName(),
                        List.of(ChatColor.GRAY + rune.getDescription())));
            } else {
                inventory.setItem(slot, icon(Material.LIGHT_GRAY_DYE,
                        ChatColor.DARK_GRAY + "No rune",
                        List.of(ChatColor.GRAY + "Engrave runes at the Rune Engraver.")));
            }
        }
    }

    private void placeAbility(
            @Nonnull Inventory inventory,
            @Nonnull Player player,
            @Nonnull ArmorState state,
            int slot,
            String abilityId,
            @Nonnull String label) {

        Ability ability = abilities.get(abilityId);
        if (ability == null) {
            inventory.setItem(slot, icon(Material.GRAY_STAINED_GLASS_PANE,
                    ChatColor.DARK_GRAY + label + ": none", List.of()));
            return;
        }

        long remaining = cooldownService.getRemainingSeconds(player.getUniqueId(), ability.getId());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + ability.getDescription());
        lore.add("");
        lore.add(ChatColor.GRAY + "Cost: " + ChatColor.AQUA
                + abilityService.getEffectiveCost(player, ability) + " energy");
        lore.add(ChatColor.GRAY + "Cooldown: " + ChatColor.WHITE
                + String.format("%.1fs", abilityService.getEffectiveCooldown(player, ability)));
        lore.add(remaining > 0
                ? ChatColor.RED + "Ready in " + remaining + "s"
                : ChatColor.GREEN + "Ready");

        if (state.piecesWorn() < ability.getTrigger().getRequiredPieces()) {
            lore.add(ChatColor.RED + "Requires the full set");
        }

        inventory.setItem(slot, icon(Material.ENCHANTED_BOOK,
                ChatColor.GOLD + label + ": " + ChatColor.WHITE + ability.getDisplayName(), lore));
    }

    @Nonnull
    private static String energyBar(@Nonnull ArmorState state) {
        int filled = (int) Math.round(state.energyFraction() * 20);
        return ChatColor.AQUA + "|".repeat(filled) + ChatColor.DARK_GRAY + "|".repeat(20 - filled);
    }

    private static void fillBackground(@Nonnull Inventory inventory) {
        ItemStack filler = icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    @Nonnull
    private static ItemStack icon(@Nonnull Material material, @Nonnull String name, @Nonnull List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
