package me.arcane.armor.core.data;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import javax.annotation.Nonnull;
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

        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not save ascension-data.yml", ex);
        }
    }
}
