package me.arcane.armor.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import me.arcane.armor.api.armor.SetTrait;
import me.arcane.armor.config.ArcaneConfig;
import me.arcane.armor.core.armor.ArmorState;
import me.arcane.armor.core.armor.EquipmentService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Applies the combat-facing traits.
 *
 * <p>Everything here reads the cached {@link ArmorState} rather than the player's inventory, which
 * matters because damage events are among the hottest on a busy server: a hit costs a map lookup,
 * not four persistent data container reads.
 */
public final class ArcaneCombatListener implements Listener {

    private static final double LIFE_STEAL_FRACTION = 0.12D;
    private static final double CHAIN_LIGHTNING_RANGE = 5.0D;
    /** How long a Molten Edge hit keeps the target burning: 60 ticks = 3 seconds. */
    private static final int MOLTEN_EDGE_TICKS = 60;
    /** Damage a Thunder Step discharge deals back to the attacker, in half-hearts. */
    private static final double THUNDER_STEP_DAMAGE = 4.0D;
    /** Standing still for this long arms the Glacial Shell. */
    private static final long GLACIAL_SHELL_IDLE_MS = 3_000L;
    /** How long after taking a hit a Phase Walk pearl costs nothing. */
    private static final long PHASE_WALK_WINDOW_MS = 10_000L;

    /** Mana Spring bubble defaults; the config can override every one of them. */
    private static final int MANA_SPRING_MAX_CHARGES = 2;
    private static final long MANA_SPRING_RECHARGE_MS = 8_000L;
    private static final double MANA_SPRING_ABSORB = 4.0D;

    private final Plugin plugin;
    private final EquipmentService equipmentService;
    private final ArcaneConfig config;
    private final ThreadLocal<Boolean> isChaining = ThreadLocal.withInitial(() -> Boolean.FALSE);
    /** Where each Glacial Shell wearer is standing and how long they have been still there. */
    private final Map<UUID, StandTracker> stands = new ConcurrentHashMap<>();
    /** When each Phase Walk window opened by swallowing a hit closes again. */
    private final Map<UUID, Long> phaseWindows = new ConcurrentHashMap<>();
    /** How many Mana Spring bubbles each wearer has banked and when they last ticked. */
    private final Map<UUID, ManaBubble> bubbles = new ConcurrentHashMap<>();

    private record StandTracker(@Nonnull Block block, long sinceMillis) {}

    /** A banked Mana Spring bubble: charges kept and the anchor for the next recharge. */
    private static final class ManaBubble {
        int charges;
        long sinceMillis;

        ManaBubble(int charges, long sinceMillis) {
            this.charges = charges;
            this.sinceMillis = sinceMillis;
        }

        /** Adds every bubble the elapsed recharge periods earned and anchors the next one. */
        void refill(int maxCharges, long rechargeMillis) {
            long now = System.currentTimeMillis();
            long earned = (now - sinceMillis) / rechargeMillis;
            if (earned <= 0) {
                return;
            }

            charges = Math.min(maxCharges, charges + (int) Math.min(earned, Integer.MAX_VALUE));
            sinceMillis += earned * rechargeMillis;
            // Idle players bank up to the cap and then stop moving the anchor, so the
            // recharges do not pile up into a distant future tick.
            if (charges >= maxCharges) {
                sinceMillis = now;
            }
        }
    }

    public ArcaneCombatListener(
            @Nonnull Plugin plugin,
            @Nonnull EquipmentService equipmentService,
            @Nonnull ArcaneConfig config) {
        this.plugin = plugin;
        this.equipmentService = equipmentService;
        this.config = config;
    }

    /** Outgoing damage: bonuses, life steal and chain lightning. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDealDamage(@Nonnull EntityDamageByEntityEvent event) {
        if (isChaining.get()) {
            return;
        }

        if (event.getEntity() instanceof ArmorStand) {
            return;
        }

        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        ArmorState state = equipmentService.getCachedState(player);
        if (!state.isWearingAny()) {
            return;
        }

        double multiplier = 1.0D + state.damageBonus();
        if (state.hasTrait(SetTrait.CRIT_BONUS) && isCritical(player)) {
            multiplier += 0.25D;
        }

        if (multiplier != 1.0D) {
            event.setDamage(event.getDamage() * multiplier);
        }

        if (state.hasTrait(SetTrait.LIFE_STEAL)) {
            heal(player, event.getFinalDamage() * LIFE_STEAL_FRACTION);
        }

        if (state.hasTrait(SetTrait.MOLTEN_EDGE)
                && event.getEntity() instanceof LivingEntity struck
                && event.getFinalDamage() > 0.0D) {
            struck.setFireTicks(Math.max(struck.getFireTicks(), MOLTEN_EDGE_TICKS));
        }

        if (state.hasTrait(SetTrait.CHAIN_LIGHTNING) && event.getEntity() instanceof LivingEntity struck) {
            if (event.getFinalDamage() > 0.0D) {
                isChaining.set(true);
                try {
                    chain(player, struck, event.getFinalDamage());
                } finally {
                    isChaining.set(false);
                }
            }
        }
    }

    /** Incoming damage: immunities, reductions and retaliation. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTakeDamage(@Nonnull EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ArmorState state = equipmentService.getCachedState(player);
        if (!state.isWearingAny()) {
            return;
        }

        if (state.hasTrait(SetTrait.PHASE_WALK)) {
            if (isPearlDamage(event)) {
                // A phased pearl never hurts: the teleport event below cleans up momentum,
                // so nothing is applied here and the damage is simply not allowed to exist.
                Long until = phaseWindows.get(player.getUniqueId());
                if (until != null && until > System.currentTimeMillis()) {
                    event.setCancelled(true);
                }
                return;
            }

            // Swallowing a hit opens the window: the next pearl thrown within it is free.
            phaseWindows.put(player.getUniqueId(), System.currentTimeMillis() + PHASE_WALK_WINDOW_MS);
        }

        if (isImmune(state, event.getCause())) {
            event.setCancelled(true);
            return;
        }

        if (state.damageReduction() > 0.0D) {
            event.setDamage(event.getDamage() * (1.0D - state.damageReduction()));
        }

        if (state.hasTrait(SetTrait.FREEZE_ATTACKERS)
                && event instanceof EntityDamageByEntityEvent byEntity
                && byEntity.getDamager() instanceof LivingEntity attacker) {

            attacker.setFreezeTicks(Math.max(attacker.getFreezeTicks(), 80));
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, true, true, true));
        }

        if (state.hasTrait(SetTrait.THUNDER_STEP)
                && !isChaining.get()
                && event instanceof EntityDamageByEntityEvent byEntity
                && byEntity.getDamager() instanceof LivingEntity attacker
                && !(attacker instanceof Player target && !target.getWorld().getPVP())) {

            // The discharge is deliberately not a full lightning strike on the target's position,
            // only the visual: damage is dealt directly, so nearby entities stay unharmed.
            isChaining.set(true);
            try {
                player.getWorld().strikeLightningEffect(attacker.getLocation());
                attacker.damage(THUNDER_STEP_DAMAGE, player);
            } finally {
                isChaining.set(false);
            }
        }

        if (state.hasTrait(SetTrait.GLACIAL_SHELL)
                && event instanceof EntityDamageByEntityEvent byEntity
                && byEntity.getDamager() instanceof LivingEntity attacker) {
            StandTracker tracker = stands.get(player.getUniqueId());
            if (tracker != null && System.currentTimeMillis() - tracker.sinceMillis() >= GLACIAL_SHELL_IDLE_MS) {
                attacker.setFreezeTicks(Math.max(attacker.getFreezeTicks(), 140));
                attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2, true, true, true));
                // Consumed: the shell rearms only after another standstill period.
                stands.put(player.getUniqueId(), new StandTracker(tracker.block(), System.currentTimeMillis()));
            }
        }

        // Mana Spring: the banked bubble eats part of the hit and pops; recharges over time.
        if (state.hasTrait(SetTrait.MANA_SPRING)) {
            absorbWithBubble(player, event);
        }
    }

    /**
     * Eats part of the hit with a banked Mana Spring bubble.
     *
     * <p>Bubbles recharge one at a time over {@code signature.mana-spring.recharge-seconds}; banked
     * while idle, spent on contact. The pop is deliberately small: confirmation, not celebration.
     *
     * <p>Runs at HIGH priority before the final damage is computed, so armor enchantments never
     * double-dip on the part of the hit the bubble removed.
     */
    private void absorbWithBubble(@Nonnull Player player, @Nonnull EntityDamageEvent event) {
        int maxCharges = Math.max(1, config.raw().getInt("signature.mana-spring.max-charges", MANA_SPRING_MAX_CHARGES));
        long rechargeMs = Math.max(1_000L, (long) (config.raw()
                .getDouble("signature.mana-spring.recharge-seconds", 8.0D) * 1000.0D));
        double absorb = Math.max(0.0D,
                config.raw().getDouble("signature.mana-spring.absorb", MANA_SPRING_ABSORB));

        UUID id = player.getUniqueId();
        ManaBubble bubble = bubbles.computeIfAbsent(id, key -> new ManaBubble(0, System.currentTimeMillis()));
        bubble.refill(maxCharges, rechargeMs);
        if (bubble.charges <= 0 || absorb <= 0.0D || event.getDamage() <= 0.0D) {
            return;
        }

        double eaten = Math.min(absorb, event.getDamage());
        event.setDamage(event.getDamage() - eaten);

        // Consumed: the next bubble starts recharging from now.
        bubble.charges--;
        bubble.sinceMillis = System.currentTimeMillis();

        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0),
                8, 0.2D, 0.3D, 0.2D, 0.02D);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.6F, 1.8F);
    }

    /** Vanilla's own definition: falling, not on the ground, not climbing, unimpaired. */
    private static boolean isImmune(@Nonnull ArmorState state, @Nonnull EntityDamageEvent.DamageCause cause) {
        return switch (cause) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR -> state.hasTrait(SetTrait.FIRE_IMMUNE);
            case LIGHTNING -> state.hasTrait(SetTrait.LIGHTNING_IMMUNE);
            case FALL -> state.hasTrait(SetTrait.FALL_IMMUNE);
            case POISON, WITHER -> state.hasTrait(SetTrait.POISON_IMMUNE);
            default -> false;
        };
    }

    /** Vanilla's own definition: falling, not on the ground, not climbing, unimpaired. */
    private static boolean isCritical(@Nonnull Player player) {
        return player.getFallDistance() > 0.0F
                && !player.isOnGround()
                && !player.isClimbing()
                && !player.isInWater()
                && !player.isSprinting();
    }

    private static void heal(@Nonnull Player player, double amount) {
        if (amount <= 0.0D) {
            return;
        }

        double max = player.getAttribute(Attribute.MAX_HEALTH) == null
                ? 20.0D
                : player.getAttribute(Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.min(max, player.getHealth() + amount));
    }

    /** Arcs a fraction of the hit to one other nearby enemy. */
    private void chain(@Nonnull Player source, @Nonnull LivingEntity struck, double damage) {        List<org.bukkit.entity.Entity> nearby = struck.getNearbyEntities(
                CHAIN_LIGHTNING_RANGE, CHAIN_LIGHTNING_RANGE, CHAIN_LIGHTNING_RANGE);

        for (org.bukkit.entity.Entity entity : nearby) {
            if (!(entity instanceof LivingEntity next)
                    || next instanceof ArmorStand
                    || next.equals(source)
                    || next.equals(struck)) {
                continue;
            }
            if (next.isDead() || !next.isValid()) {
                continue;
            }
            if (next instanceof Player target && !target.getWorld().getPVP()) {
                continue;
            }

            next.getWorld().strikeLightningEffect(next.getLocation());
            next.damage(damage * 0.4D, source);
            return;
        }
    }

    // ---- Tier V and Ascended trait handlers ---------------------------------------------------

    /** A phased pearl lands without the self-damage or the throw cooldown. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPearlThrow(@Nonnull ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)
                || !(pearl.getShooter() instanceof Player player)) {
            return;
        }

        Long until = phaseWindows.get(player.getUniqueId());
        if (until == null || until <= System.currentTimeMillis()) {
            return;
        }

        // Vanilla sets the 20-tick cooldown when the pearl leaves the hand, so the refund has to
        // run one tick after that.
        Bukkit.getScheduler().runTask(plugin, () -> player.setCooldown(Material.ENDER_PEARL, 0));
    }

    /** A phased pearl arrival sheds momentum and marks the landing with a portal shimmer. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(@Nonnull PlayerTeleportEvent event) {
        if (event.getCause() != TeleportCause.ENDER_PEARL) {
            return;
        }

        Player player = event.getPlayer();
        Long until = phaseWindows.get(player.getUniqueId());
        if (until == null || until <= System.currentTimeMillis()) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            player.setFallDistance(0.0F);
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0),
                    60, 0.3D, 0.8D, 0.3D, 1.0D);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7F, 1.4F);
        });
    }

    private static boolean isPearlDamage(@Nonnull EntityDamageEvent event) {
        var source = event.getDamageSource();
        if (source != null && source.getDamageType() == DamageType.ENDER_PEARL) {
            return true;
        }
        return event instanceof EntityDamageByEntityEvent byEntity
                && byEntity.getDamager() instanceof EnderPearl;
    }

    /** Tracks standstill for the Glacial Shell against block-position changes only. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(@Nonnull PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!equipmentService.getCachedState(player).hasTrait(SetTrait.GLACIAL_SHELL)) {
            stands.remove(player.getUniqueId());
            return;
        }

        Location to = event.getTo();
        if (to == null) {
            return;
        }

        Block block = to.getBlock();
        UUID id = player.getUniqueId();
        StandTracker tracker = stands.get(id);
        if (tracker == null || !sameBlock(tracker.block(), block)) {
            stands.put(id, new StandTracker(block, System.currentTimeMillis()));
        }
    }

    @EventHandler
    public void onQuit(@Nonnull PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        stands.remove(id);
        phaseWindows.remove(id);
        bubbles.remove(id);
    }

    /**
     * Arcane Bond: an Ascended-largest set keeps its worn pieces through death.
     *
     * <p>The trait only exists when every worn piece of the set is Ascended, so the pieces that
     * still match the worn armor content are exactly the ones the ladder protects. Drops removed
     * from {@code getDrops()} are handed to {@code getItemsToKeep()}, which Paper provides for
     * exactly this purpose.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(@Nonnull PlayerDeathEvent event) {
        Player player = event.getEntity();
        ArmorState state = equipmentService.getCachedState(player);
        if (!state.hasTrait(SetTrait.ARCANE_BOND)) {
            return;
        }

        ItemStack[] worn = player.getInventory().getArmorContents();
        List<ItemStack> kept = new ArrayList<>();
        for (ItemStack drop : event.getDrops()) {
            if (drop == null || !isArmorMaterial(drop.getType())) {
                continue;
            }
            for (ItemStack piece : worn) {
                if (piece != null && piece.isSimilar(drop)) {
                    kept.add(drop);
                    break;
                }
            }
        }

        if (!kept.isEmpty()) {
            event.getDrops().removeAll(kept);
            event.getItemsToKeep().addAll(kept);
        }
    }

    private static boolean sameBlock(@Nonnull Block first, @Nonnull Block second) {
        return first.getX() == second.getX()
                && first.getY() == second.getY()
                && first.getZ() == second.getZ()
                && first.getWorld().equals(second.getWorld());
    }

    private static boolean isArmorMaterial(@Nonnull Material material) {
        return material.name().endsWith("_HELMET")
                || material.name().endsWith("_CHESTPLATE")
                || material.name().endsWith("_LEGGINGS")
                || material.name().endsWith("_BOOTS");
    }
}
