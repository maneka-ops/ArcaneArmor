package me.arcane.armor.core.ability;

import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.arcane.armor.api.ability.Ability;
import me.arcane.armor.api.ability.AbilityContext;
import me.arcane.armor.api.ability.AbilityTrigger;
import me.arcane.armor.api.armor.ArmorSetDefinition;
import me.arcane.armor.api.event.AbilityUseEvent;
import me.arcane.armor.api.event.ArcaneEnergyChangeEvent;
import me.arcane.armor.api.registry.Registry;
import me.arcane.armor.config.ArcaneConfig;
import me.arcane.armor.config.Messages;
import me.arcane.armor.core.armor.ArmorState;
import me.arcane.armor.core.armor.EquipmentService;
import me.arcane.armor.core.cooldown.CooldownService;
import me.arcane.armor.core.cosmetic.CosmeticService;
import me.arcane.armor.core.energy.EnergyService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * The one place a cast is allowed to happen.
 *
 * <p>Listeners, commands and the API all funnel through {@link #cast}, which runs the checks in a
 * fixed order — enabled, permission, worn pieces, ability gate, cooldown, energy, event — and only
 * then charges the player and runs the ability. Keeping that order in a single method is what
 * stops a new trigger from accidentally skipping a check.
 */
public final class AbilityService {

    private final Plugin plugin;
    private final ArcaneConfig config;
    private final Messages messages;
    private final Registry<Ability> abilities;
    private final EquipmentService equipmentService;
    private final EnergyService energyService;
    private final CooldownService cooldownService;
    private final CosmeticService cosmeticService;

    /**
     * Global runtime switch for every ability.
     *
     * <p>An admin can silence all abilities without editing config or rebooting; players still
     * wear, upgrade and charge their armor, only the casts stop. Starts from
     * {@code abilities.enabled} in the config and is reset to it by {@code /arcane reload}, so a
     * server restart or a reload can always undo a forgotten toggle.
     */
    private volatile boolean allEnabled;

    public AbilityService(
            @Nonnull Plugin plugin,
            @Nonnull ArcaneConfig config,
            @Nonnull Messages messages,
            @Nonnull Registry<Ability> abilities,
            @Nonnull EquipmentService equipmentService,
            @Nonnull EnergyService energyService,
            @Nonnull CooldownService cooldownService,
            @Nonnull CosmeticService cosmeticService) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.abilities = abilities;
        this.equipmentService = equipmentService;
        this.energyService = energyService;
        this.cooldownService = cooldownService;
        this.cosmeticService = cosmeticService;
        this.allEnabled = config.isAllAbilitiesEnabled();
    }

    /** Whether casting is currently allowed for everyone. */
    public boolean isAllEnabled() {
        return allEnabled;
    }

    /** Turns every ability on or off, for all players, until changed again or a reload resets it. */
    public void setAllEnabled(boolean value) {
        this.allEnabled = value;
    }

    /** Result of a cast attempt, so callers can react without re-deriving the reason. */
    public enum Result {
        SUCCESS,
        NO_ARMOR,
        NO_ABILITY,
        DISABLED,
        NO_PERMISSION,
        NOT_ENOUGH_PIECES,
        ON_COOLDOWN,
        NOT_ENOUGH_ENERGY,
        UNUSABLE,
        CANCELLED
    }

    /**
     * Fires the ability bound to the given trigger for whatever set the player is wearing.
     *
     * <p>Main thread only.
     */
    @Nonnull
    public Result cast(@Nonnull Player player, @Nonnull AbilityTrigger trigger) {
        ArmorState state = equipmentService.getCachedState(player);
        ArmorSetDefinition set = state.set();
        if (set == null || !state.isWearingAny()) {
            return Result.NO_ARMOR;
        }

        String abilityId = trigger == AbilityTrigger.ULTIMATE
                ? set.getUltimateAbilityId()
                : set.getActiveAbilityId();

        Ability ability = abilities.get(abilityId);
        if (ability == null) {
            return Result.NO_ABILITY;
        }

        return cast(player, ability, state);
    }

    /**
     * Fires a specific ability, still applying every gate.
     *
     * <p>Main thread only.
     */
    @Nonnull
    public Result cast(@Nonnull Player player, @Nonnull Ability ability, @Nullable ArmorState providedState) {
        ArmorState state = providedState != null ? providedState : equipmentService.getCachedState(player);
        ArmorSetDefinition set = state.set();
        if (set == null) {
            return Result.NO_ARMOR;
        }

        // The global switch outranks everything else, even per-ability config entries, so one
        // command is enough to silence every set's casts at once.
        if (!allEnabled) {
            messages.actionBar(player, "&cArcane abilities are disabled.");
            return Result.DISABLED;
        }

        if (!config.isAbilityEnabled(ability)) {
            return Result.DISABLED;
        }

        if (config.isPermissionRequired() && !player.hasPermission("arcane.use")) {
            messages.send(player, "no-permission", "&cYou lack permission to use Arcane abilities.");
            return Result.NO_PERMISSION;
        }

        if (state.piecesWorn() < ability.getTrigger().getRequiredPieces()) {
            messages.send(player, "need-full-set", "&cThat ability needs the full set.");
            cosmeticService.denied(player);
            return Result.NOT_ENOUGH_PIECES;
        }

        long remaining = cooldownService.getRemainingSeconds(player.getUniqueId(), ability.getId());
        if (remaining > 0L) {
            if (config.isActionBarEnabled()) {
                messages.actionBar(player, "&c" + ability.getDisplayName() + " &7ready in &c" + remaining + "s");
            }
            cosmeticService.denied(player);
            return Result.ON_COOLDOWN;
        }

        // With energy disabled the armor costs nothing to use and cooldowns alone pace abilities.
        boolean energyGated = config.isEnergyEnabled();
        int cost = energyGated
                ? (int) Math.round(config.getEnergyCost(ability) * (1.0D - state.energyDiscount()))
                : 0;

        AbilityContext context =
                new AbilityContext(player, set, state.tier(), state.piecesWorn(), cost, plugin);

        if (!ability.canUse(context)) {
            cosmeticService.denied(player);
            return Result.UNUSABLE;
        }

        if (energyGated && state.energy() < cost) {
            messages.send(player, "not-enough-energy", "&cNot enough Arcane Energy. &7({energy}/{cost})",
                    Map.of("energy", String.valueOf(state.energy()), "cost", String.valueOf(cost)));
            cosmeticService.denied(player);
            return Result.NOT_ENOUGH_ENERGY;
        }

        AbilityUseEvent event = new AbilityUseEvent(player, ability, set, cost);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return Result.CANCELLED;
        }

        if (energyGated && event.getEnergyCost() > 0
                && !energyService.consume(player, event.getEnergyCost(), ArcaneEnergyChangeEvent.Cause.ABILITY)) {
            return Result.NOT_ENOUGH_ENERGY;
        }

        double cooldown = config.getCooldownSeconds(ability) * state.tier().getCooldownMultiplier();
        cooldownService.start(player.getUniqueId(), ability.getId(), cooldown);

        cosmeticService.castBurst(player, set);
        ability.execute(new AbilityContext(player, set, state.tier(), state.piecesWorn(),
                event.getEnergyCost(), plugin));

        if (config.isActionBarEnabled()) {
            String suffix = energyGated
                    ? " &8| &b" + energyService.getEnergy(player) + " &7energy"
                    : " &8| &7ready in &f" + Math.round(cooldown) + "s";
            messages.actionBar(player, set.getColor() + ability.getDisplayName() + suffix);
        }

        return Result.SUCCESS;
    }

    /** Effective cooldown for a player, accounting for their tier. */
    public double getEffectiveCooldown(@Nonnull Player player, @Nonnull Ability ability) {
        ArmorState state = equipmentService.getCachedState(player);
        return config.getCooldownSeconds(ability) * state.tier().getCooldownMultiplier();
    }

    /** Effective energy cost for a player, accounting for module discounts. */
    public int getEffectiveCost(@Nonnull Player player, @Nonnull Ability ability) {
        ArmorState state = equipmentService.getCachedState(player);
        return (int) Math.round(config.getEnergyCost(ability) * (1.0D - state.energyDiscount()));
    }
}
