package me.arcane.armor.core.energy;

import java.util.Map;
import javax.annotation.Nonnull;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.api.armor.ArmorSlot;
import me.arcane.armor.api.event.ArcaneEnergyChangeEvent;
import me.arcane.armor.core.armor.ArmorState;
import me.arcane.armor.core.armor.EquipmentService;
import me.arcane.armor.core.data.ArmorData;
import me.arcane.armor.core.data.ArmorDataService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Moves Arcane Energy in and out of worn armor.
 *
 * <p>Energy is stored per piece rather than per player, which keeps it attached to the gear: a
 * chestplate someone hands you arrives with whatever charge it had. The player's usable pool is
 * simply the sum across the worn pieces, and spending drains the fullest piece first so no single
 * piece bottoms out while the others sit full.
 *
 * <p>Every method here mutates live inventory items and must run on the main thread.
 */
public final class EnergyService {

    private final ArmorDataService dataService;
    private final EquipmentService equipmentService;

    public EnergyService(@Nonnull ArmorDataService dataService, @Nonnull EquipmentService equipmentService) {
        this.dataService = dataService;
        this.equipmentService = equipmentService;
    }

    /** Total energy across the player's worn set, from the cached state. */
    public int getEnergy(@Nonnull Player player) {
        return equipmentService.getCachedState(player).energy();
    }

    public int getCapacity(@Nonnull Player player) {
        return equipmentService.getCachedState(player).capacity();
    }

    public boolean hasEnergy(@Nonnull Player player, int amount) {
        return getEnergy(player) >= amount;
    }

    /**
     * Spends energy across the worn pieces.
     *
     * @return {@code true} if the full amount was drained; {@code false} leaves everything untouched
     */
    public boolean consume(@Nonnull Player player, int amount, @Nonnull ArcaneEnergyChangeEvent.Cause cause) {
        if (amount <= 0) {
            return true;
        }

        ArmorState state = equipmentService.getCachedState(player);
        if (state.set() == null || state.energy() < amount) {
            return false;
        }

        Map<ArmorSlot, ItemStack> worn = equipmentService.getWornPieces(player, state.set());
        int remaining = amount;

        // Drain the fullest piece first so charge stays level across the set.
        for (ItemStack piece : worn.values().stream()
                .sorted((left, right) -> Integer.compare(storedEnergy(right), storedEnergy(left)))
                .toList()) {

            if (remaining <= 0) {
                break;
            }
            remaining -= drain(player, piece, remaining, cause);
        }

        equipmentService.refresh(player);
        return remaining <= 0;
    }

    /**
     * Adds energy to the worn pieces, filling the emptiest first.
     *
     * @return how much was actually stored
     */
    public int charge(@Nonnull Player player, int amount, @Nonnull ArcaneEnergyChangeEvent.Cause cause) {
        if (amount <= 0) {
            return 0;
        }

        ArmorState state = equipmentService.getCachedState(player);
        ArmorSetDefinition set = state.set();
        if (set == null) {
            return 0;
        }

        Map<ArmorSlot, ItemStack> worn = equipmentService.getWornPieces(player, set);
        int remaining = amount;

        for (ItemStack piece : worn.values().stream()
                .sorted((left, right) -> Integer.compare(storedEnergy(left), storedEnergy(right)))
                .toList()) {

            if (remaining <= 0) {
                break;
            }
            remaining -= fill(player, piece, set, remaining, cause);
        }

        equipmentService.refresh(player);
        return amount - remaining;
    }

    /**
     * Charges a single item outside of any player's inventory, used by the Crystal Charger.
     *
     * @return how much was stored
     */
    public int chargeItem(@Nonnull ItemStack item, @Nonnull ArmorSetDefinition set, int amount) {
        return dataService.read(item)
                .map(data -> {
                    int capacity = data.tier().getEnergyCapacity(set.getBaseEnergyCapacity());
                    int stored = Math.min(amount, capacity - data.energy());
                    if (stored > 0) {
                        dataService.write(item, data.withEnergy(data.energy() + stored));
                    }
                    return stored;
                })
                .orElse(0);
    }

    private int drain(
            @Nonnull Player player,
            @Nonnull ItemStack piece,
            int wanted,
            @Nonnull ArcaneEnergyChangeEvent.Cause cause) {

        ArmorData data = dataService.read(piece).orElse(null);
        if (data == null || data.energy() <= 0) {
            return 0;
        }

        int taken = Math.min(wanted, data.energy());
        int updated = data.energy() - taken;

        ArcaneEnergyChangeEvent event = new ArcaneEnergyChangeEvent(player, piece, cause, data.energy(), updated);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return 0;
        }

        dataService.write(piece, data.withEnergy(event.getNewEnergy()));
        return data.energy() - event.getNewEnergy();
    }

    private int fill(
            @Nonnull Player player,
            @Nonnull ItemStack piece,
            @Nonnull ArmorSetDefinition set,
            int wanted,
            @Nonnull ArcaneEnergyChangeEvent.Cause cause) {

        ArmorData data = dataService.read(piece).orElse(null);
        if (data == null) {
            return 0;
        }

        int capacity = data.tier().getEnergyCapacity(set.getBaseEnergyCapacity());
        int room = capacity - data.energy();
        if (room <= 0) {
            return 0;
        }

        int added = Math.min(wanted, room);
        ArcaneEnergyChangeEvent event =
                new ArcaneEnergyChangeEvent(player, piece, cause, data.energy(), data.energy() + added);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return 0;
        }

        dataService.write(piece, data.withEnergy(event.getNewEnergy()));
        return event.getNewEnergy() - data.energy();
    }

    private int storedEnergy(@Nonnull ItemStack item) {
        return dataService.read(item).map(ArmorData::energy).orElse(0);
    }
}
