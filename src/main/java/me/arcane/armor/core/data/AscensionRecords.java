package me.arcane.armor.core.data;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Who was first to bring a piece of each set to Ascended tier.
 *
 * <p>One entry per set id, persisted to {@code ascension-data.yml} the moment it is claimed, so a
 * restart never lets two players fight over the same milestone. The claim is taken on whatever
 * thread the Upgrade Altar ticks on, because the record only matters as "first", while the chat
 * announcement is scheduled back onto the main thread by the caller.
 */
public final class AscensionRecords {

    private final Plugin plugin;
    private final File file;

    /** Set id to the name of the first player to Ascend a piece of it. */
    private final Map<String, String> firsts = new ConcurrentHashMap<>();

    /** Player id to the number of pieces they have brought to Ascended tier. */
    private final Map<UUID, Integer> ascendedCounts = new ConcurrentHashMap<>();

    public AscensionRecords(@Nonnull Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "ascension-data.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("first-ascended");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            firsts.put(key.toLowerCase(Locale.ROOT), section.getString(key, "an unnamed craftsman"));
        }

        ConfigurationSection counts = yaml.getConfigurationSection("ascended-count");
        if (counts == null) {
            return;
        }

        for (String key : counts.getKeys(false)) {
            try {
                ascendedCounts.put(UUID.fromString(key), counts.getInt(key));
            } catch (IllegalArgumentException ignored) {
                // A malformed id is dropped rather than bricking the file; counts rebuild naturally.
            }
        }
    }

    /**
     * Claims the milestone for this set and player.
     *
     * @return {@code true} when this player was the first ever to Ascend a piece of the set
     */
    public synchronized boolean claim(@Nonnull String setId, @Nonnull String playerName) {
        if (firsts.putIfAbsent(setId.toLowerCase(Locale.ROOT), playerName) != null) {
            return false;
        }
        save();
        return true;
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, String> entry : firsts.entrySet()) {
            yaml.set("first-ascended." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<UUID, Integer> entry : ascendedCounts.entrySet()) {
            yaml.set("ascended-count." + entry.getKey(), entry.getValue());
        }

        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not save ascension-data.yml", ex);
        }
    }

    /**
     * Clears first-to-Ascend claims: one set, or every set when {@code null}.
     *
     * <p>Claims are usually taken only from the Upgrade Altar's successful runs, but a support or
     * testing session may have inked one in; the next Ascended piece re-claims the milestone.
     */
    public synchronized void reset(@Nullable String setId) {
        if (setId == null) {
            firsts.clear();
        } else {
            firsts.remove(setId.toLowerCase(Locale.ROOT));
        }
        save();
    }

    /**
     * Counts one more Ascended piece for this player, written straight through to disk.
     *
     * <p>Unlike the first-to-Ascend claims this is an achievement counter, not a one-time
     * milestone, so {@link #reset(String)} never touches it; the leaderboard depends on the sum
     * being stable across milestone resets.
     */
    public synchronized void recordAscended(@Nonnull UUID playerId) {
        ascendedCounts.merge(playerId, 1, Integer::sum);
        save();
    }

    /** How many pieces this player has brought to Ascended tier. */
    public int countAscended(@Nonnull UUID playerId) {
        return ascendedCounts.getOrDefault(playerId, 0);
    }

    /** Every player id known to these records, for leaderboards that walk them. */
    @Nonnull
    public Set<UUID> trackedPlayers() {
        return new HashSet<>(ascendedCounts.keySet());
    }
}
