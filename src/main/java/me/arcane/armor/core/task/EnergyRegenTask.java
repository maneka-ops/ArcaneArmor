package me.arcane.armor.core.task;

import javax.annotation.Nonnull;
import me.arcane.armor.api.event.ArcaneEnergyChangeEvent;
import me.arcane.armor.api.armor.SetTrait;
import me.arcane.armor.config.ArcaneConfig;
import me.arcane.armor.core.armor.ArmorState;
import me.arcane.armor.core.armor.EquipmentService;
import me.arcane.armor.core.energy.EnergyService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Trickles Arcane Energy back into worn armor.
 *
 * <p>Deliberately slow: passive regeneration is the floor that stops a player from being stranded
 * with a dead set, while the Crystal Charger and charged crystals remain the fast path. Sleeping
 * multiplies the trickle, which gives the night a use beyond skipping monsters.
 *
 * <p>Runs on the main thread because it writes to live inventory items.
 */
public final class EnergyRegenTask implements Runnable {

    private final ArcaneConfig config;
    private final EquipmentService equipmentService;
    private final EnergyService energyService;

    public EnergyRegenTask(
            @Nonnull ArcaneConfig config,
            @Nonnull EquipmentService equipmentService,
            @Nonnull EnergyService energyService) {
        this.config = config;
        this.equipmentService = equipmentService;
        this.energyService = energyService;
    }

    @Override
    public void run() {
        // Nothing spends energy while it is switched off, so nothing needs to refill it either.
        if (!config.isEnergyEnabled()) {
            return;
        }

        int baseAmount = config.getEnergyRegenAmount();
        if (baseAmount <= 0) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            ArmorState state = equipmentService.getCachedState(player);
            if (!state.isWearingAny() || state.energy() >= state.capacity()) {
                continue;
            }

            int amount = player.isSleeping()
                    ? (int) Math.round(baseAmount * config.getSleepRegenMultiplier())
                    : baseAmount;

            // Mana Spring (Apprentice signature): the set that lives off its energy stream
            // refills noticeably faster while it is worn.
            if (state.hasTrait(SetTrait.MANA_SPRING)) {
                double multiplier = Math.max(1.0D,
                        config.raw().getDouble("signature.mana-spring.regen-multiplier", 2.0D));
                amount = (int) Math.round(amount * multiplier);
            }

            energyService.charge(player, amount, ArcaneEnergyChangeEvent.Cause.REGENERATION);
        }
    }
}
