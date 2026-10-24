package me.arcane.armor.core.quest;

import java.util.List;
import javax.annotation.Nonnull;

/**
 * The quest that unlocks crafting one armor set at the Arcane Forge.
 *
 * <p>Objective order is significant: progress is stored per index, so reordering a quest's
 * objectives in {@code config.yml} moves progress between them. Adding or removing objectives is
 * safe — the stored counts are resized on read.
 *
 * @param rewardLevels experience levels granted on completion
 * @param rewardCommands console commands run on completion, with {@code {player}} replaced
 */
public record ArmorQuest(
        @Nonnull String setId,
        @Nonnull List<QuestObjective> objectives,
        int rewardLevels,
        @Nonnull List<String> rewardCommands) {

    public ArmorQuest {
        objectives = List.copyOf(objectives);
        rewardLevels = Math.max(0, rewardLevels);
        rewardCommands = List.copyOf(rewardCommands);
    }

    public boolean hasDeliveries() {
        return objectives.stream().anyMatch(objective -> objective.type() == QuestObjective.Type.DELIVER);
    }
}
