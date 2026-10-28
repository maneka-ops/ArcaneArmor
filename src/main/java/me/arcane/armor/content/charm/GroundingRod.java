package me.arcane.armor.content.charm;

import javax.annotation.Nonnull;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import me.arcane.armor.config.Messages;
import me.arcane.armor.core.combat.InsulatorService;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * The Grounding Rod: the counter-play to Storm's free passive lightning.
 *
 * <p>Right-clicking grounds the holder for a minute — chain arcs skip them, Thunder Step never
 * discharges at them, real lightning bolts do nothing, and a Storm wearer's direct hits land at
 * half strength. The rod itself then sits on a cooldown, so a fight stays a fight: the charm buys
 * breathing room against mounted pressure, it does not make Storm unhit.
 *
 * <p>The rod is consumed on a successful activation but not on a failed one, so an impatient
 * second click that lands inside the cooldown never wastes a charge.
 */
public class GroundingRod extends SlimefunItem {

    private final InsulatorService insulator;
    private final Messages messages;

    public GroundingRod(
            @Nonnull ItemGroup itemGroup,
            @Nonnull SlimefunItemStack item,
            @Nonnull RecipeType recipeType,
            @Nonnull ItemStack[] recipe,
            @Nonnull InsulatorService insulator,
            @Nonnull Messages messages) {
        super(itemGroup, item, recipeType, recipe);
        this.insulator = insulator;
        this.messages = messages;
    }

    @Override
    public void preRegister() {
        super.preRegister();

        addItemHandler((ItemUseHandler) event -> {
            Player player = event.getPlayer();
            ItemStack held = stackInHand(player, event);
            if (held == null) {
                return;
            }

            long cooldown = insulator.cooldownSecondsLeft(player.getUniqueId());
            if (cooldown > 0) {
                // Spent charge: nothing is consumed, the player only learns when to try again.
                messages.actionBar(player, "&cThe rod must recover: &e" + cooldown + "s");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6F, 0.6F);
                event.cancel();
                return;
            }

            if (insulator.isActive(player.getUniqueId())) {
                messages.actionBar(player, "&7You are grounded for another &e"
                        + insulator.secondsLeft(player.getUniqueId()) + "s&7.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6F, 0.6F);
                event.cancel();
                return;
            }

            insulator.activate(player.getUniqueId());
            held.setAmount(held.getAmount() - 1);

            messages.actionBar(player, "&e&lGROUNDED! &7Storm's passive lightning cannot touch you for &e"
                    + insulator.secondsLeft(player.getUniqueId()) + "s&7.");
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0F, 1.4F);
        });
    }

    /** Works out which hand the click came from, since the decrement needs the real stack. */
    private ItemStack stackInHand(@Nonnull Player player, @Nonnull PlayerRightClickEvent event) {
        EquipmentSlot hand = event.getHand();
        if (hand == EquipmentSlot.OFF_HAND) {
            return player.getInventory().getItemInOffHand();
        }
        return player.getInventory().getItemInMainHand();
    }
}
