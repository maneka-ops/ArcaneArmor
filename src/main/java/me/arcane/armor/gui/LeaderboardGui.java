package me.arcane.armor.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.arcane.armor.core.data.AscensionRecords;
import me.arcane.armor.core.quest.QuestService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * The {@code /arcane top} leaderboard: one head per ranked player, ordered by Ascended pieces
 * first and finished quests second.
 *
 * <p>The ranking is a snapshot taken when the menu opens, sorted purely from persisted data —
 * no live player has to be online to hold a rank. {@link GuiListener} makes it read-only.
 */
public final class LeaderboardGui {

    /** Entry rows: four interior rows of seven, leaving the frame and the footer free. */
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43};
    private static final int SIZE = 54;
    private static final int SELF_SLOT = 49;

    private final QuestService questService;
    private final AscensionRecords ascensionRecords;

    public LeaderboardGui(@Nonnull QuestService questService, @Nonnull AscensionRecords ascensionRecords) {
        this.questService = questService;
        this.ascensionRecords = ascensionRecords;
    }

    /** One ranked player. */
    public record Entry(
            @Nonnull UUID playerId,
            @Nullable String name,
            int ascended,
            int quests) {

        @Nonnull
        public String displayName() {
            return name == null ? "Unknown" : name;
        }
    }

    /**
     * Every ranked player, best first. Naming resolves through the server's own cache only, so
     * opening the menu never performs a blocking profile lookup.
     */
    @Nonnull
    public List<Entry> ranking() {
        Set<UUID> candidates = new HashSet<>(ascensionRecords.trackedPlayers());
        candidates.addAll(questService.trackedPlayers());

        Map<UUID, Entry> entries = new HashMap<>();
        for (UUID id : candidates) {
            int ascended = ascensionRecords.countAscended(id);
            int quests = questService.countUnlocked(id);
            if (ascended == 0 && quests == 0) {
                continue;
            }

            Player online = Bukkit.getPlayer(id);
            OfflinePlayer offline = online == null ? Bukkit.getOfflinePlayer(id) : null;
            String name = online != null ? online.getName()
                    : offline != null ? offline.getName()
                    : null;
            entries.put(id, new Entry(id, name, ascended, quests));
        }

        List<Entry> ranked = new ArrayList<>(entries.values());
        ranked.sort(Comparator
                .comparingInt(Entry::ascended).reversed()
                .thenComparing(Comparator.comparingInt(Entry::quests).reversed())
                .thenComparing(entry -> entry.displayName().toLowerCase()));
        return ranked;
    }

    public void open(@Nonnull Player player) {
        List<Entry> ranked = ranking();

        ArcaneMenuHolder holder = new ArcaneMenuHolder(ArcaneMenuHolder.Type.LEADERBOARD, null);
        Inventory inventory = Bukkit.createInventory(holder, SIZE,
                net.kyori.adventure.text.Component.text("Arcane Leaderboard"));
        holder.setInventory(inventory);

        ItemStack filler = icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, filler);
        }

        for (int index = 0; index < ranked.size() && index < SLOTS.length; index++) {
            inventory.setItem(SLOTS[index], playerIcon(ranked.get(index), index + 1));
        }

        inventory.setItem(SELF_SLOT, selfIcon(ranked, player));
        player.openInventory(inventory);
    }

    @Nonnull
    private ItemStack playerIcon(@Nonnull Entry entry, int rank) {
        String rankColor = rank == 1 ? ChatColor.GOLD + ""
                : rank == 2 ? ChatColor.YELLOW + ""
                : rank == 3 ? ChatColor.WHITE + ""
                : ChatColor.GRAY + "";
        String title = rankColor + "#" + rank + " " + entry.displayName();
        return icon(headFor(entry), title, List.of(
                "",
                ChatColor.LIGHT_PURPLE + "Ascended pieces: " + ChatColor.WHITE + entry.ascended(),
                ChatColor.AQUA + "Quests completed: " + ChatColor.WHITE + entry.quests(),
                "",
                ChatColor.DARK_GRAY + "Pieces are counted when an",
                ChatColor.DARK_GRAY + "Upgrade Altar finishes them."));
    }
    @Nonnull
    private ItemStack headFor(@Nonnull Entry entry) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        Player online = Bukkit.getPlayer(entry.playerId());
        OfflinePlayer profile = online != null ? online : Bukkit.getOfflinePlayer(entry.playerId());
        meta.setOwningPlayer(profile);
        head.setItemMeta(meta);
        return head;
    }

    @Nonnull
    private ItemStack selfIcon(@Nonnull List<Entry> ranked, @Nonnull Player viewer) {
        int rank = 0;
        for (int index = 0; index < ranked.size(); index++) {
            if (ranked.get(index).playerId().equals(viewer.getUniqueId())) {
                rank = index + 1;
                break;
            }
        }

        List<String> lore = new ArrayList<>();
        lore.add("");
        if (rank == 0) {
            lore.add(ChatColor.WHITE + "You are not ranked yet.");
            lore.add(ChatColor.GRAY + "Ascend a piece at an Upgrade Altar");
            lore.add(ChatColor.GRAY + "and your name appears here.");
        } else {
            lore.add(ChatColor.LIGHT_PURPLE + "Your rank: " + ChatColor.WHITE + "#" + rank);
            lore.add(ChatColor.GRAY + "Ascend more pieces to climb.");
        }
        return icon(Material.NETHER_STAR, ChatColor.LIGHT_PURPLE + "Your standings", lore);
    }

    @Nonnull
    private static ItemStack icon(@Nonnull Material material, @Nonnull String name, @Nonnull List<String> lore) {
        return icon(new ItemStack(material), name, lore);
    }

    @Nonnull
    private static ItemStack icon(@Nonnull ItemStack item, @Nonnull String name, @Nonnull List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}
