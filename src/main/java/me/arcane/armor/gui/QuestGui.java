package me.arcane.armor.gui;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.core.quest.ArmorQuest;
import me.arcane.armor.core.quest.QuestObjective;
import me.arcane.armor.core.quest.QuestService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Read-only list of the armor quests: one icon per set, in chain order, showing whether the set is
 * unlocked, which objectives are left, or which quest has to be finished first.
 *
 * <p>The one interaction is clicking a quest with delivery objectives, which hands in matching items
 * from the player's inventory. {@link GuiListener} cancels every click first, so nothing can be
 * taken out of the menu either way.
 */
public final class QuestGui implements Listener {

    /** Middle row of a three-row chest, centred for the five bundled sets. */
    private static final int[] SLOTS = {11, 12, 13, 14, 15, 10, 16, 19, 20, 21, 22, 23, 24, 25};
    private static final int SIZE = 36;
    private static final int BAR_LENGTH = 20;

    private final QuestService questService;

    public QuestGui(@Nonnull QuestService questService) {
        this.questService = questService;
    }

    public void open(@Nonnull Player player) {
        ArcaneMenuHolder holder = new ArcaneMenuHolder(ArcaneMenuHolder.Type.QUESTS, null);
        Inventory inventory = Bukkit.createInventory(holder, SIZE,
                net.kyori.adventure.text.Component.text("Armor Quests"));
        holder.setInventory(inventory);

        ItemStack filler = icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, filler);
        }

        drawQuests(inventory, player);

        inventory.setItem(31, icon(Material.BOOK, ChatColor.LIGHT_PURPLE + "How quests work", List.of(
                ChatColor.GRAY + "Each armor set has a quest.",
                ChatColor.GRAY + "Finish it to unlock forging that",
                ChatColor.GRAY + "set at your Arcane Forge.",
                "",
                ChatColor.GRAY + "Quests unlock one after another,",
                ChatColor.GRAY + "following the armor ladder.",
                "",
                ChatColor.GRAY + "Click a quest to deliver items",
                ChatColor.GRAY + "it asks for from your inventory.")));

        player.openInventory(inventory);
    }

    private void drawQuests(@Nonnull Inventory inventory, @Nonnull Player player) {
        List<ArmorSetDefinition> sets = questService.orderedSets();
        for (int index = 0; index < sets.size() && index < SLOTS.length; index++) {
            inventory.setItem(SLOTS[index], questIcon(player, sets.get(index)));
        }
    }

    @EventHandler
    public void onClick(@Nonnull InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof ArcaneMenuHolder holder)
                || holder.getType() != ArcaneMenuHolder.Type.QUESTS
                || event.getClickedInventory() != top
                || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        List<ArmorSetDefinition> sets = questService.orderedSets();
        for (int index = 0; index < sets.size() && index < SLOTS.length; index++) {
            if (SLOTS[index] == event.getSlot()) {
                questService.deliver(player, sets.get(index));
                // Redrawn in place: finishing one quest can unlock the next one's icon too.
                drawQuests(top, player);
                return;
            }
        }
    }

    @Nonnull
    private ItemStack questIcon(@Nonnull Player player, @Nonnull ArmorSetDefinition set) {
        String title = set.getColor() + set.getDisplayName() + " Armor";
        ArmorQuest quest = questService.getQuest(set.getId());
        QuestService.Status status = questService.getStatus(player.getUniqueId(), set);

        List<String> lore = new ArrayList<>();
        lore.add("");

        switch (status) {
            case COMPLETE -> {
                lore.add(ChatColor.GREEN + "✔ Unlocked");
                lore.add(ChatColor.GRAY + "You can forge this set.");
                return glowing(icon(set.getThemeMaterial(), title, lore));
            }
            case LOCKED -> {
                lore.add(ChatColor.RED + "✘ Locked");
                lore.add(ChatColor.GRAY + "Finish the " + previousName(set) + ChatColor.GRAY + " quest first.");
                appendRequirement(lore, player, set);
                appendObjectives(lore, player, quest, false);
                return icon(Material.GRAY_DYE, title, lore);
            }
            default -> {
                lore.add(ChatColor.YELLOW + "➤ In progress");
                appendRequirement(lore, player, set);
                appendObjectives(lore, player, quest, true);
                appendRewards(lore, quest);
                if (questService.getDeathPenaltyPercent() > 0) {
                    lore.add(ChatColor.DARK_RED + "Dying loses " + questService.getDeathPenaltyPercent()
                            + "% of unfinished progress.");
                }
                if (quest != null && quest.hasDeliveries()) {
                    lore.add("");
                    lore.add(ChatColor.LIGHT_PURPLE + "▶ Click to deliver items");
                }
                return icon(set.getThemeMaterial(), title, lore);
            }
        }
    }

    /** The armor the player has to be wearing for this quest to count, and whether they are. */
    private void appendRequirement(@Nonnull List<String> lore, @Nonnull Player player, @Nonnull ArmorSetDefinition set) {
        String requirement = questService.describeRequirement(set);
        if (requirement == null) {
            return;
        }
        lore.add("");
        lore.add(ChatColor.GRAY + "Must be wearing " + ChatColor.WHITE + requirement);
        lore.add(questService.isWearingRequired(player, set)
                ? ChatColor.GREEN + " ✔ Wearing it now"
                : ChatColor.RED + " ✘ Not wearing it - progress paused");
    }

    private void appendObjectives(
            @Nonnull List<String> lore,
            @Nonnull Player player,
            ArmorQuest quest,
            boolean showProgress) {

        if (quest == null) {
            return;
        }

        lore.add("");
        lore.add(ChatColor.GRAY + "Objectives:");
        for (int index = 0; index < quest.objectives().size(); index++) {
            QuestObjective objective = quest.objectives().get(index);
            int done = showProgress
                    ? Math.min(objective.amount(), questService.getProgress(player.getUniqueId(), quest.setId(), index))
                    : 0;
            boolean finished = done >= objective.amount();

            lore.add((finished ? ChatColor.GREEN + " ✔ " : ChatColor.DARK_GRAY + " • ")
                    + ChatColor.WHITE + objective.describe());
            if (showProgress && !finished) {
                int filled = Math.round(BAR_LENGTH * (float) done / objective.amount());
                lore.add("   " + ChatColor.GREEN + "|".repeat(filled)
                        + ChatColor.DARK_GRAY + "|".repeat(BAR_LENGTH - filled)
                        + ChatColor.GRAY + " " + done + "/" + objective.amount());
            }
        }
    }

    private static void appendRewards(@Nonnull List<String> lore, ArmorQuest quest) {
        if (quest == null || (quest.rewardLevels() == 0 && quest.rewardCommands().isEmpty())) {
            return;
        }
        lore.add("");
        lore.add(ChatColor.GOLD + "Rewards:");
        if (quest.rewardLevels() > 0) {
            lore.add(ChatColor.WHITE + " • " + quest.rewardLevels() + " experience levels");
        }
        if (!quest.rewardCommands().isEmpty()) {
            lore.add(ChatColor.WHITE + " • Bonus rewards");
        }
    }

    @Nonnull
    private String previousName(@Nonnull ArmorSetDefinition set) {
        for (ArmorSetDefinition candidate : questService.orderedSets()) {
            if (candidate.getId().equals(set.getPreviousSetId())) {
                return candidate.getColor() + candidate.getDisplayName();
            }
        }
        return String.valueOf(set.getPreviousSetId());
    }

    @Nonnull
    private static ItemStack glowing(@Nonnull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    @Nonnull
    private static ItemStack icon(@Nonnull Material material, @Nonnull String name, @Nonnull List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}
