package me.arcane.armor.core.task;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.api.armor.ArmorTier;
import me.arcane.armor.api.armor.PassiveEffect;
import me.arcane.armor.api.module.ArmorModule;
import me.arcane.armor.config.ArcaneConfig;
import me.arcane.armor.core.armor.ArmorState;
import me.arcane.armor.core.armor.EquipmentService;
import me.arcane.armor.core.cosmetic.CosmeticService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * The single heartbeat of the addon.
 *
 * <p>One task walks every online player once per interval, recomputes their armor state and
 * reapplies passives. Running it as one task rather than one per player (or per effect) keeps the
 * cost proportional to the player count and gives the whole plugin a single place to profile.
 *
 * <p>Effects are granted with a duration slightly longer than the interval so they never visibly
 * flicker, and are re-issued only when the amplifier changes or the effect is close to lapsing —
 * re-adding an identical effect every second would otherwise spam clients with packets.
 */
public final class ArcaneTickTask implements Runnable {

    private final ArcaneConfig config;
    private final EquipmentService equipmentService;
    private final CosmeticService cosmeticService;

    public ArcaneTickTask(
            @Nonnull ArcaneConfig config,
            @Nonnull EquipmentService equipmentService,
            @Nonnull CosmeticService cosmeticService) {
        this.config = config;
        this.equipmentService = equipmentService;
        this.cosmeticService = cosmeticService;
    }

    @Override
    public void run() {
        int duration = config.getTickInterval() + 40;

        for (Player player : Bukkit.getOnlinePlayers()) {
            ArmorState state = equipmentService.refresh(player);
            if (!state.isWearingAny()) {
                continue;
            }

            applyEffects(player, state, duration);

            ArmorSetDefinition set = state.set();
            if (set != null && state.isFullSet()) {
                cosmeticService.aura(player, set);
            }
        }
    }

    private void applyEffects(@Nonnull Player player, @Nonnull ArmorState state, int duration) {
        Map<PotionEffectType, Integer> strongest = new HashMap<>();

        // Tier IV unlock: every full-set passive climbs one amplifier once the weakest worn piece
        // has reached it, so upgrading the last piece visibly upgrades the whole set.
        int amplifierBoost = state.tier().ordinal() >= ArmorTier.TIER_4.ordinal() ? 1 : 0;

        ArmorSetDefinition set = state.set();
        if (set != null) {
            for (PassiveEffect passive : set.getPassives()) {
                if (state.piecesWorn() >= passive.minPieces()) {
                    merge(strongest, passive.type(), passive.amplifier() + amplifierBoost);
                }
            }
        }

        for (ArmorModule module : state.modules()) {
            module.getEffects().forEach((type, amplifier) -> merge(strongest, type, amplifier));
        }

        strongest.forEach((type, amplifier) -> {
            PotionEffect current = player.getPotionEffect(type);
            int threshold = config.getTickInterval() + 10;

            boolean needsRefresh = current == null
                    || current.getAmplifier() < amplifier
                    || current.getDuration() <= threshold;

            if (needsRefresh) {
                player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, false));
            }
        });
    }

    /** Overlapping sources do not stack; the strongest one wins. */
    private static void merge(
            @Nonnull Map<PotionEffectType, Integer> target,
            @Nonnull PotionEffectType type,
            int amplifier) {
        target.merge(type, amplifier, Math::max);
    }
}
