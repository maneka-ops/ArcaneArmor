package me.arcane.armor.core.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.arcane.armor.api.armor.ArmorTier;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Reads and writes {@link ArmorData} on item stacks through the persistent data container.
 *
 * <p>Pure item I/O with no player state, which keeps it callable from anywhere: the tick task,
 * the machines, the GUIs and the command handler all funnel through here so the storage format
 * has exactly one owner.
 */
public final class ArmorDataService {

    private static final String SEPARATOR = ";";

    private final NamespacedKey setKey;
    private final NamespacedKey tierKey;
    private final NamespacedKey energyKey;
    private final NamespacedKey modulesKey;
    private final NamespacedKey runesKey;

    public ArmorDataService(@Nonnull Plugin plugin) {
        this.setKey = new NamespacedKey(plugin, "set");
        this.tierKey = new NamespacedKey(plugin, "tier");
        this.energyKey = new NamespacedKey(plugin, "energy");
        this.modulesKey = new NamespacedKey(plugin, "modules");
        this.runesKey = new NamespacedKey(plugin, "runes");
    }

    /** Cheap check that avoids the full read when a caller only needs a yes/no. */
    public boolean isArcaneArmor(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(setKey, PersistentDataType.STRING);
    }

    @Nonnull
    public Optional<ArmorData> read(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Optional.empty();
        }

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        String setId = container.get(setKey, PersistentDataType.STRING);
        if (setId == null) {
            return Optional.empty();
        }

        ArmorTier tier = ArmorTier.fromId(container.get(tierKey, PersistentDataType.STRING), ArmorTier.TIER_1);
        Integer energy = container.get(energyKey, PersistentDataType.INTEGER);

        return Optional.of(new ArmorData(
                setId,
                tier,
                energy == null ? 0 : energy,
                split(container.get(modulesKey, PersistentDataType.STRING)),
                split(container.get(runesKey, PersistentDataType.STRING))));
    }

    /** Writes the whole snapshot back onto the item, replacing any previous state. */
    public void write(@Nonnull ItemStack item, @Nonnull ArmorData data) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(setKey, PersistentDataType.STRING, data.setId());
        container.set(tierKey, PersistentDataType.STRING, data.tier().name());
        container.set(energyKey, PersistentDataType.INTEGER, data.energy());
        container.set(modulesKey, PersistentDataType.STRING, join(data.moduleIds()));
        container.set(runesKey, PersistentDataType.STRING, join(data.runeIds()));

        item.setItemMeta(meta);
    }

    /**
     * Reads, transforms and writes in one step.
     *
     * @return the updated data, or empty if the item carried no Arcane state
     */
    @Nonnull
    public Optional<ArmorData> update(@Nullable ItemStack item, @Nonnull UnaryOperator<ArmorData> mutator) {
        if (item == null) {
            return Optional.empty();
        }

        Optional<ArmorData> current = read(item);
        if (current.isEmpty()) {
            return Optional.empty();
        }

        ArmorData updated = mutator.apply(current.get());
        write(item, updated);
        return Optional.of(updated);
    }

    @Nonnull
    private static List<String> split(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>(Arrays.asList(raw.split(SEPARATOR)));
        values.removeIf(String::isBlank);
        return values;
    }

    @Nonnull
    private static String join(@Nonnull List<String> values) {
        return String.join(SEPARATOR, values);
    }
}
