package me.arcane.armor.api.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Thread-safe registry of {@link Keyed} values.
 *
 * <p>Registration happens on the main thread during startup and is sealed with {@link #freeze()}
 * once the addon finishes loading; lookups afterwards are lock-free and safe from any thread,
 * including the async tasks that resolve abilities and modules off the ticking path.
 *
 * @param <T> the registered type
 */
public final class Registry<T extends Keyed> {

    private final String name;
    private final Map<String, T> values = new ConcurrentHashMap<>();
    private final AtomicBoolean frozen = new AtomicBoolean();

    public Registry(@Nonnull String name) {
        this.name = name;
    }

    /**
     * Registers a value.
     *
     * @throws IllegalStateException if the registry is frozen
     * @throws IllegalArgumentException if the id is already taken
     */
    @Nonnull
    public T register(@Nonnull T value) {
        if (frozen.get()) {
            throw new IllegalStateException(name + " registry is frozen, register during onEnable");
        }

        T previous = values.putIfAbsent(value.getId(), value);
        if (previous != null) {
            throw new IllegalArgumentException(name + " already contains '" + value.getId() + "'");
        }
        return value;
    }

    @Nullable
    public T get(@Nullable String id) {
        return id == null ? null : values.get(id);
    }

    @Nonnull
    public Optional<T> find(@Nullable String id) {
        return Optional.ofNullable(get(id));
    }

    public boolean contains(@Nullable String id) {
        return id != null && values.containsKey(id);
    }

    @Nonnull
    public Collection<T> values() {
        return Collections.unmodifiableCollection(values.values());
    }

    public int size() {
        return values.size();
    }

    /** Seals the registry. Idempotent, so a reload that re-runs setup will not blow up. */
    public void freeze() {
        frozen.set(true);
    }

    public boolean isFrozen() {
        return frozen.get();
    }

    @Nonnull
    public String getName() {
        return name;
    }
}
