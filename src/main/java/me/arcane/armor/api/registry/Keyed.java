package me.arcane.armor.api.registry;

import javax.annotation.Nonnull;

/**
 * Anything that can live in a {@link Registry}.
 *
 * <p>The id doubles as the config path and as the value persisted on armor items, so it must be
 * stable across releases: renaming an id orphans every item that already carries it.
 */
public interface Keyed {

    /**
     * @return a lowercase, snake_case identifier that is unique within its registry
     */
    @Nonnull
    String getId();
}
