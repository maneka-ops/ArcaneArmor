package me.arcane.armor.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import me.arcane.armor.api.armor.SetTrait;
import me.arcane.armor.core.armor.ArmorState;
import me.arcane.armor.core.armor.EquipmentService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
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

    private final EquipmentService equipmentService;
    private final ThreadLocal<Boolean> isChaining = ThreadLocal.withInitial(() -> Boolean.FALSE);
    /** Where each Glacial Shell wearer is standing and how long they have been still there. */
    private final Map<UUID, StandTracker> stands = new ConcurrentHashMap<>();

    private record StandTracker(@Nonnull Block block, long sinceMillis) {}

    public ArcaneCombatListener(@Nonnull EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
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
    }

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
        stands.remove(event.getPlayer().getUniqueId());
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
