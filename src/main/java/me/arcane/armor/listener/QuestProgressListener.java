package me.arcane.armor.listener;

import javax.annotation.Nonnull;
import me.arcane.armor.api.event.AbilityUseEvent;
import me.arcane.armor.config.ArcaneConfig;
import me.arcane.armor.core.quest.QuestObjective;
import me.arcane.armor.core.quest.QuestService;
import org.bukkit.GameMode;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Feeds kills, mined blocks and ability casts into the armor quests, and applies the death penalty.
 *
 * <p>Both handlers run at MONITOR and skip cancelled events, so a kill or break that a protection
 * plugin blocked never counts. The two cheap farms are closed off: spawner mobs (unless the config
 * allows them) and silk-touched blocks, which could otherwise be placed and mined again forever.
 */
public final class QuestProgressListener implements Listener {

    private final QuestService questService;
    private final ArcaneConfig config;

    public QuestProgressListener(@Nonnull QuestService questService, @Nonnull ArcaneConfig config) {
        this.questService = questService;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(@Nonnull EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null || entity instanceof Player || entity instanceof ArmorStand) {
            return;
        }

        if (entity.fromMobSpawner() && !config.raw().getBoolean("quests.count-spawner-mobs", false)) {
            return;
        }

        questService.record(killer, QuestObjective.Type.KILL, entity.getType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(@Nonnull BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
            return;
        }

        questService.record(player, QuestObjective.Type.MINE, event.getBlock().getType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAbility(@Nonnull AbilityUseEvent event) {
        questService.record(event.getPlayer(), QuestObjective.Type.ABILITY, event.getAbility().getId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(@Nonnull PlayerDeathEvent event) {
        questService.applyDeathPenalty(event.getEntity());
    }
}
