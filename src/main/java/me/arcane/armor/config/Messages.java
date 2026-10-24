package me.arcane.armor.config;

import java.util.Map;
import javax.annotation.Nonnull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Player-facing strings, all sourced from {@code config.yml} under {@code messages}.
 *
 * <p>Legacy ampersand codes are accepted because that is what server owners expect to type, and
 * are converted to Adventure components on the way out so nothing goes through the deprecated
 * string APIs.
 */
public final class Messages {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final ArcaneConfig config;

    public Messages(@Nonnull ArcaneConfig config) {
        this.config = config;
    }

    /** Raw string for a key, with the prefix already applied where the default carries one. */
    @Nonnull
    public String raw(@Nonnull String key, @Nonnull String fallback) {
        return config.raw().getString("messages." + key, fallback);
    }

    @Nonnull
    public Component get(@Nonnull String key, @Nonnull String fallback) {
        return SERIALIZER.deserialize(prefix() + raw(key, fallback));
    }

    /**
     * Formats a message, replacing every {@code {placeholder}} with its value.
     */
    @Nonnull
    public Component get(@Nonnull String key, @Nonnull String fallback, @Nonnull Map<String, String> placeholders) {
        String message = prefix() + raw(key, fallback);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return SERIALIZER.deserialize(message);
    }

    public void send(@Nonnull CommandSender target, @Nonnull String key, @Nonnull String fallback) {
        target.sendMessage(get(key, fallback));
    }

    public void send(
            @Nonnull CommandSender target,
            @Nonnull String key,
            @Nonnull String fallback,
            @Nonnull Map<String, String> placeholders) {
        target.sendMessage(get(key, fallback, placeholders));
    }

    public void actionBar(@Nonnull Player player, @Nonnull String legacyText) {
        player.sendActionBar(SERIALIZER.deserialize(legacyText));
    }

    /** Deserializes an arbitrary legacy string without touching the prefix. */
    @Nonnull
    public Component legacy(@Nonnull String legacyText) {
        return SERIALIZER.deserialize(legacyText);
    }

    @Nonnull
    private String prefix() {
        return config.raw().getString("messages.prefix", "&5&lArcane &8» &7");
    }
}
