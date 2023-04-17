package net.skinsrestorer.api.storage;

import java.util.Optional;
import java.util.UUID;

public interface CacheStorage {
    /**
     * Gets the uuid of a Mojang player from the cache.
     * If the uuid is not found locally, it will try to get the uuid from Mojang.
     *
     * @param playerName Player name to search for
     * @return The uuid of the player or empty if not found
     */
    Optional<UUID> getUUID(String playerName);
}
