package me.arcane.armor.core.cosmetic;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Per-player switch for the cosmetic layer.
 *
 * <p>One bit per player, persisted to {@code fx-data.yml} the moment it changes. Only the
 * effects a player emits around themselves are muted — server-wide milestones still reach
 * everyone, because those are announcements rather than decoration.
 */
public final class FxPreferences {

    private final Plugin plugin;
    private final File file;

    /** Player ids who asked for their own particle and sound effects to stay off. */
    private final Map<UUID, Boolean> disabled = new ConcurrentHashMap<>();

    public FxPreferences(@Nonnull Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "fx-data.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            if (key.startsWith("fx-disabled.")) {
                try {
                    disabled.put(UUID.fromString(key.substring("fx-disabled.".length())), Boolean.TRUE);
                } catch (IllegalArgumentException ignored) {
                    // A malformed key is not worth blocking the load over; it is simply dropped.
                }
            }
        }
    }

    /** Whether this player has muted their own effects. */
    public boolean isDisabled(@Nonnull UUID playerId) {
        return disabled.containsKey(playerId);
    }

    /** Flips the switch and returns the new state: {@code true} when effects are now muted. */
    public boolean toggle(@Nonnull UUID playerId) {
        boolean nowDisabled = !isDisabled(playerId);
        if (nowDisabled) {
            disabled.put(playerId, Boolean.TRUE);
        } else {
            disabled.remove(playerId);
        }
        save();
        return nowDisabled;
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (UUID id : disabled.keySet()) {
            yaml.set("fx-disabled." + id, true);
        }

        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not save fx-data.yml", ex);
        }
    }
}
