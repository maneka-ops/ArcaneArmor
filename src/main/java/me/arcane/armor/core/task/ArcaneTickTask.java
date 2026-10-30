package me.arcane.armor.core.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
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

    /**
     * Storm's Speed badge shows one level above what it moves at. Vanilla implements the Speed
     * buff as a Multiply-Total movement-speed modifier worth {@code +0.2 x (level)}, so a badge of
     * III adds {@code +0.6} while a true II adds {@code +0.4}; trimming the total by 12.5% turns
     * {@code 1.6 x 0.875 = 1.4} — the II figure — back in behind the III badge.
     */
    private static final double SPEED_TRIM = -0.125D;
    private static final UUID SPEED_TRIM_KEY = UUID.fromString("7ba3a0d8-2361-4fd8-9c13-4b5c1c68ba39");

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

        ArmorSetDefinition set = state.set();
        boolean storm = set != null && "storm".equals(set.getId());

        // Tier IV unlock: every full-set passive climbs one amplifier once the weakest worn piece
        // has reached it, so upgrading the last piece visibly upgrades the whole set.
        //
        // Storm's Speed and Regen badges take that climb one step further in public — the badge
        // reads III — while the substance stays at the level the set always promised: Speed moves
        // like II (the movement-speed trim below) and Regen heals at the II rate (the combat
        // listener neutralizes the vanilla drip and this task paces it). Haste stays honestly II.
        int amplifierBoost = state.tier().ordinal() >= ArmorTier.TIER_4.ordinal() ? 1 : 0;

        if (set != null) {
            for (PassiveEffect passive : set.getPassives()) {
                if (state.piecesWorn() < passive.minPieces()) {
                    continue;
                }
                int amplifier = passive.amplifier() + amplifierBoost;
                if (storm && amplifierBoost > 0
                        && passive.type() == PotionEffectType.HASTE) {
                    amplifier = passive.amplifier();
                }
                merge(strongest, passive.type(), amplifier);
            }
        }

        for (ArmorModule module : state.modules()) {
            module.getEffects().forEach((type, amplifier) -> merge(strongest, type, amplifier));
        }

        // The badge potions: our own faces get a visible badge so the dressed-up level actually
        // reads on the client. Everything else keeps the quiet ambient treatment.
        strongest.forEach((type, amplifier) -> {
            PotionEffect current = player.getPotionEffect(type);
            int threshold = config.getTickInterval() + 10;

            boolean needsRefresh = current == null
                    || current.getAmplifier() < amplifier
                    || current.getDuration() <= threshold;

            if (needsRefresh) {
                boolean badgeVisible = storm
                        && (type == PotionEffectType.SPEED || type == PotionEffectType.REGENERATION)
                        && amplifier >= 2;
                player.addPotionEffect(new PotionEffect(
                        type, duration, amplifier, true, false, badgeVisible));
            }
        });

        updateSpeedTrim(player, storm && strongest.getOrDefault(PotionEffectType.SPEED, -1) == 2);

        if (storm && strongest.getOrDefault(PotionEffectType.REGENERATION, -1) == 2
                && player.getHealth() > 0.0D) {
            healAtPassiveRate(player);
        }
    }

    /**
     * Keeps the Storm Speed badge honest at its promised move speed: the trim rides along only
     * while a III badge is being dressed, and is removed the moment it is not.
     */
    private void updateSpeedTrim(@Nonnull Player player, boolean active) {
        AttributeInstance attribute = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }

        boolean present = false;
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (SPEED_TRIM_KEY.equals(modifier.getUniqueId())) {
                present = true;
                break;
            }
        }

        if (active && !present) {
            attribute.addModifier(new AttributeModifier(
                    SPEED_TRIM_KEY,
                    "arcane_storm_speed_trim",
                    SPEED_TRIM,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        } else if (!active && present) {
            attribute.removeModifier(SPEED_TRIM_KEY);
        }
    }

    /**
     * The Regen badge drip: a real Regen II averages {@code 0.8} HP per second, so the badge's
     * dressed-up III is paced to exactly that here, while the combat listener keeps vanilla's
     * III-speed procs from landing on their own.
     */
    private void healAtPassiveRate(@Nonnull Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        double max = maxHealth == null ? 20.0D : maxHealth.getValue();
        double health = player.getHealth();
        if (health > 0.0D && health < max) {
            double budget = 0.8D * (config.getTickInterval() / 20.0D);
            player.setHealth(Math.min(max, health + budget));
        }
    }

    /** Overlapping sources do not stack; the strongest one wins. */
    private static void merge(
            @Nonnull Map<PotionEffectType, Integer> target,
            @Nonnull PotionEffectType type,
            int amplifier) {
        target.merge(type, amplifier, Math::max);
    }
}
