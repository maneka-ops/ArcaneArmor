package me.arcane.armor.integration;

import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

/**
 * Optional third-party support, resolved once at startup.
 *
 * <p>ItemsAdder, Oraxen and MythicMobs are reached through reflection rather than compile-time
 * dependencies. That keeps the jar buildable and loadable without any of them installed, and means
 * a version bump on their side cannot stop this addon from enabling — at worst a lookup returns
 * {@code null} and the caller falls back to vanilla behaviour.
 */
public final class Integrations {

    private final boolean itemsAdder;
    private final boolean oraxen;
    private final boolean mythicMobs;
    private final boolean placeholderApi;
    private final Logger logger;

    public Integrations(@Nonnull Logger logger) {
        this.logger = logger;
        this.itemsAdder = isEnabled("ItemsAdder");
        this.oraxen = isEnabled("Oraxen");
        this.mythicMobs = isEnabled("MythicMobs");
        this.placeholderApi = isEnabled("PlaceholderAPI");
    }

    public boolean hasItemsAdder() {
        return itemsAdder;
    }

    public boolean hasOraxen() {
        return oraxen;
    }

    public boolean hasMythicMobs() {
        return mythicMobs;
    }

    public boolean hasPlaceholderApi() {
        return placeholderApi;
    }

    /**
     * Resolves a custom item by id from whichever pack plugin is installed.
     *
     * @param id namespaced id, e.g. {@code itemsadder:ruby} or a plain Oraxen id
     * @return the item, or {@code null} if no provider knows it
     */
    @Nullable
    public ItemStack resolveCustomItem(@Nonnull String id) {
        if (itemsAdder) {
            ItemStack resolved = viaItemsAdder(id);
            if (resolved != null) {
                return resolved;
            }
        }
        if (oraxen) {
            return viaOraxen(id);
        }
        return null;
    }

    @Nullable
    private ItemStack viaItemsAdder(@Nonnull String id) {
        try {
            Class<?> customStack = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Object stack = customStack.getMethod("getInstance", String.class).invoke(null, id);
            if (stack == null) {
                return null;
            }
            return (ItemStack) customStack.getMethod("getItemStack").invoke(stack);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            logger.fine(() -> "ItemsAdder lookup failed for '" + id + "': " + exception.getMessage());
            return null;
        }
    }

    @Nullable
    private ItemStack viaOraxen(@Nonnull String id) {
        try {
            Class<?> itemsClass = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
            Object builder = itemsClass.getMethod("getItemById", String.class).invoke(null, id);
            if (builder == null) {
                return null;
            }
            return (ItemStack) builder.getClass().getMethod("build").invoke(builder);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            logger.fine(() -> "Oraxen lookup failed for '" + id + "': " + exception.getMessage());
            return null;
        }
    }

    /** Logs what was found, once, so server owners can confirm their setup at a glance. */
    public void logSummary() {
        logger.info("Integrations: PlaceholderAPI=" + placeholderApi
                + " ItemsAdder=" + itemsAdder
                + " Oraxen=" + oraxen
                + " MythicMobs=" + mythicMobs);
    }

    private static boolean isEnabled(@Nonnull String pluginName) {
        return Bukkit.getPluginManager().isPluginEnabled(pluginName);
    }
}
