/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.skinsrestorer.shared.storage;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.storage.CacheStorage;
import net.skinsrestorer.shared.config.StorageConfig;
import net.skinsrestorer.shared.connections.MojangAPIImpl;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.model.cache.MojangCacheData;
import net.skinsrestorer.shared.utils.C;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CacheStorageImpl implements CacheStorage {
    private final SRLogger logger;
    private final MojangAPIImpl mojangAPI;
    private final SettingsManager settings;
    @Setter
    private StorageAdapter storageAdapter;

    @Override
    public Optional<UUID> getUUID(String playerName) {
        if (!C.validMojangUsername(playerName)) {
            return Optional.empty();
        }

        try {
            Optional<MojangCacheData> stored = storageAdapter.getCachedUUID(playerName);

            if (stored.isPresent() && !isExpiredUniqueId(stored.get().getTimestampSeconds())) {
                return stored.map(MojangCacheData::getUniqueId);
            }

            try {
                Optional<UUID> uuid = mojangAPI.getUUID(playerName);

                uuid.ifPresent(value -> storageAdapter.setCachedUUID(playerName,
                        MojangCacheData.of(value, playerName, Instant.now().getEpochSecond())));

                return uuid;
            } catch (DataRequestException e) {
                logger.debug("Failed to get UUID from Mojang for " + playerName, e);
                return stored.map(MojangCacheData::getUniqueId);
            }
        } catch (StorageAdapter.StorageException e) {
            logger.warning("Failed to get UUID from cache for " + playerName, e);
            return Optional.empty();
        }
    }

    private boolean isExpiredUniqueId(long epochSecond) {
        int expiresAfter = settings.getProperty(StorageConfig.UUID_EXPIRES_AFTER);
        return expiresAfter > 0 && Instant.now().getEpochSecond() - epochSecond > expiresAfter;
    }
}
