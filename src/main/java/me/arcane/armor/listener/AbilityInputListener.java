package me.arcane.armor.listener;

import javax.annotation.Nonnull;
import me.arcane.armor.api.ability.AbilityTrigger;
import me.arcane.armor.core.ability.AbilityService;
import me.arcane.armor.core.armor.EquipmentService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * Binds the swap-hand key to ability casting.
 *
 * <p>Swap-hand is the only input a plugin can read reliably without a client mod or a resource
 * pack, and it is rebindable in vanilla, so it is what the addon uses: tap to cast the set's
 * active, sneak and tap for its ultimate. The event is only cancelled when a cast actually
 * happens, so players not wearing Arcane armor keep their off-hand behaviour.
 */
public final class AbilityInputListener implements Listener {

    private final AbilityService abilityService;
    private final EquipmentService equipmentService;

    public AbilityInputListener(
            @Nonnull AbilityService abilityService,
            @Nonnull EquipmentService equipmentService) {
        this.abilityService = abilityService;
        this.equipmentService = equipmentService;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSwapHands(@Nonnull PlayerSwapHandItemsEvent event) {
        if (!equipmentService.getCachedState(event.getPlayer()).isWearingAny()) {
            return;
        }

        AbilityTrigger trigger = event.getPlayer().isSneaking()
                ? AbilityTrigger.ULTIMATE
                : AbilityTrigger.ACTIVE;

        AbilityService.Result result = abilityService.cast(event.getPlayer(), trigger);

        // Anything other than "this set has no such ability" means we handled the key press.
        if (result != AbilityService.Result.NO_ABILITY && result != AbilityService.Result.NO_ARMOR) {
            event.setCancelled(true);
        }
    }
}
