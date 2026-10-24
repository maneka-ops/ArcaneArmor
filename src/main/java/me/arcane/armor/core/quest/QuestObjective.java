package me.arcane.armor.core.quest;

import java.util.Locale;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * One step of an armor quest: slay, mine, cast or deliver some amount of a set of targets,
 * optionally only in one dimension or only at night.
 *
 * <p>Targets are stored as upper-case names rather than resolved {@code EntityType} or
 * {@code Material} values, so matching is a set lookup and one objective can cover every variant of
 * a block — {@code LAPIS_ORE} and {@code DEEPSLATE_LAPIS_ORE} — without a special case.
 *
 * @param dimension where the objective counts, or null for anywhere
 * @param nightOnly whether it only counts in the Overworld between dusk and dawn
 */
public record QuestObjective(
        @Nonnull Type type,
        @Nonnull Set<String> targets,
        int amount,
        @Nonnull String label,
        @Nullable World.Environment dimension,
        boolean nightOnly) {

    /** Target that matches every ability, for {@link Type#ABILITY} objectives. */
    public static final String ANY = "ANY";

    /** Overworld time span that counts as night, matching when hostile mobs spawn. */
    private static final long DUSK = 13_000L;
    private static final long DAWN = 23_000L;

    /** What the player has to do. */
    public enum Type {
        /** Kill an entity whose type is in the target list. */
        KILL,
        /** Break a block whose material is in the target list. */
        MINE,
        /** Cast an Arcane ability whose id is in the target list, or any ability for {@code ANY}. */
        ABILITY,
        /** Hand in items from the quest menu: Slimefun item ids or vanilla materials. */
        DELIVER
    }

    public QuestObjective {
        targets = Set.copyOf(targets);
        amount = Math.max(1, amount);
    }

    public boolean matches(@Nonnull Type type, @Nonnull String target) {
        if (this.type != type) {
            return false;
        }
        String upper = target.toUpperCase(Locale.ROOT);
        return targets.contains(upper) || (type == Type.ABILITY && targets.contains(ANY));
    }

    /** Whether the player is somewhere, and at a time, this objective counts. */
    public boolean meetsConditions(@Nonnull Player player) {
        World world = player.getWorld();
        if (dimension != null && world.getEnvironment() != dimension) {
            return false;
        }
        if (nightOnly) {
            long time = world.getTime();
            return world.getEnvironment() == World.Environment.NORMAL && time >= DUSK && time < DAWN;
        }
        return true;
    }

    /** Player-facing line, e.g. {@code Slay 30 Zombies at night in the Overworld}. */
    @Nonnull
    public String describe() {
        String base = switch (type) {
            case KILL -> "Slay " + amount + " " + label;
            case MINE -> "Mine " + amount + " " + label;
            case ABILITY -> "Use " + label + " " + amount + " times";
            case DELIVER -> "Deliver " + amount + " " + label;
        };
        return base + describeConditions();
    }

    @Nonnull
    private String describeConditions() {
        StringBuilder builder = new StringBuilder();
        if (nightOnly) {
            builder.append(" at night");
        }
        if (dimension != null) {
            builder.append(switch (dimension) {
                case NETHER -> " in the Nether";
                case THE_END -> " in the End";
                default -> " in the Overworld";
            });
        }
        return builder.toString();
    }
}
