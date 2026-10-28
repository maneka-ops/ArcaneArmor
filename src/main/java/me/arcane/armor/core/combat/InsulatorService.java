package me.arcane.armor.core.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

/**
 * The Grounding Rod's shield: who is grounded, until when, and whose rod is still recovering.
 *
 * <p>One lookup answers the combat listener's questions, and the whole state is volatile, so a
 * restart naturally finds everyone ungrounded and un-cooled. Activation failures come back as the
 * remaining cooldown so the item handler can quote it without a second map walk.
 */
public final class InsulatorService {

    /** How long the charm keeps a player grounded: one minute. */
    private static final long SHIELD_MS = 60_000L;
    /** How long the rod itself needs to cool after one use. */
    private static final long COOLDOWN_MS = 120_000L;

    private final Map<UUID, Insulator> players = new ConcurrentHashMap<>();

    private record Insulator(long groundedUntil, long readyAt) {}

    /**
     * Activates the shield.
     *
     * @return {@code false} when the player's rod is still cooling down, {@code true} otherwise
     */
    public boolean activate(@Nonnull UUID playerId) {
        long now = System.currentTimeMillis();
        Insulator current = players.get(playerId);
        if (current != null && current.readyAt > now) {
            return false;
        }

        players.put(playerId, new Insulator(now + SHIELD_MS, now + COOLDOWN_MS));
        return true;
    }

    /** Whether this player's shield still stands. */
    public boolean isActive(@Nonnull UUID playerId) {
        Insulator current = players.get(playerId);
        return current != null && current.groundedUntil > System.currentTimeMillis();
    }

    /** Seconds of grounding still left, for HUD feedback. */
    public long secondsLeft(@Nonnull UUID playerId) {
        Insulator current = players.get(playerId);
        if (current == null) {
            return 0L;
        }
        return Math.max(0L, (current.groundedUntil - System.currentTimeMillis()) / 1000L);
    }

    /** Seconds until the rod can be used again; {@code 0} when it is ready now. */
    public long cooldownSecondsLeft(@Nonnull UUID playerId) {
        Insulator current = players.get(playerId);
        if (current == null) {
            return 0L;
        }
        return Math.max(0L, (current.readyAt - System.currentTimeMillis()) / 1000L);
    }

    /** Drops the player's entry; called on logout so the map cannot grow without bound. */
    public void clear(@Nonnull UUID playerId) {
        players.remove(playerId);
    }
}
