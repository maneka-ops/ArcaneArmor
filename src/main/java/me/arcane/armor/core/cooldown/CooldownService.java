package me.arcane.armor.core.cooldown;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

/**
 * Per-player, per-ability cooldown clock.
 *
 * <p>Stores expiry timestamps rather than counting down, so nothing has to tick and a lag spike
 * cannot stretch a cooldown. Entries are purged for a player on quit and swept periodically, which
 * is what keeps this from turning into a slow leak on a busy server.
 */
public final class CooldownService {

    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    /** Whether the ability is ready for this player. */
    public boolean isReady(@Nonnull UUID playerId, @Nonnull String abilityId) {
        return getRemainingMillis(playerId, abilityId) <= 0L;
    }

    public long getRemainingMillis(@Nonnull UUID playerId, @Nonnull String abilityId) {
        Map<String, Long> byAbility = cooldowns.get(playerId);
        if (byAbility == null) {
            return 0L;
        }

        Long expiry = byAbility.get(abilityId);
        if (expiry == null) {
            return 0L;
        }

        long remaining = expiry - System.currentTimeMillis();
        if (remaining <= 0L) {
            byAbility.remove(abilityId);
            return 0L;
        }
        return remaining;
    }

    /** Remaining time rounded up to whole seconds, which is what players see. */
    public long getRemainingSeconds(@Nonnull UUID playerId, @Nonnull String abilityId) {
        return (getRemainingMillis(playerId, abilityId) + 999L) / 1000L;
    }

    public void start(@Nonnull UUID playerId, @Nonnull String abilityId, double seconds) {
        if (seconds <= 0.0D) {
            return;
        }
        cooldowns.computeIfAbsent(playerId, key -> new ConcurrentHashMap<>())
                .put(abilityId, System.currentTimeMillis() + (long) (seconds * 1000.0D));
    }

    public void clear(@Nonnull UUID playerId, @Nonnull String abilityId) {
        Map<String, Long> byAbility = cooldowns.get(playerId);
        if (byAbility != null) {
            byAbility.remove(abilityId);
        }
    }

    public void clear(@Nonnull UUID playerId) {
        cooldowns.remove(playerId);
    }

    public void clearAll() {
        cooldowns.clear();
    }

    /**
     * Drops expired entries and players with nothing left pending.
     *
     * <p>Safe to run async: every map involved is concurrent and no Bukkit API is touched.
     */
    public void purgeExpired() {
        long now = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(entry -> {
            entry.getValue().values().removeIf(expiry -> expiry <= now);
            return entry.getValue().isEmpty();
        });
    }

    public int trackedPlayers() {
        return cooldowns.size();
    }
}
