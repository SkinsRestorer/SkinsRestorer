/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.MojangSkinDataResult;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.CacheStorage;
import net.skinsrestorer.shared.config.StorageConfig;
import net.skinsrestorer.shared.connections.MojangAPIImpl;
import net.skinsrestorer.shared.exception.DataRequestExceptionShared;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.storage.adapter.AtomicAdapter;
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
    private final AtomicAdapter atomicAdapter;

    @Override
    public Optional<MojangSkinDataResult> getSkin(String playerName, boolean allowExpired) throws DataRequestException {
        if (!C.validMojangUsername(playerName)) {
            return Optional.empty();
        }

        try {
            Optional<MojangCacheData> stored = getCachedData(playerName, allowExpired);
            if (stored.isPresent()) {
                if (stored.get().isPremium()) {
                    Optional<SkinProperty> skinProperty = mojangAPI.getProfile(stored.get().getUniqueId());

                    return skinProperty.map(property -> MojangSkinDataResult.of(stored.get().getUniqueId(), property));
                } else {
                    return Optional.empty();
                }
            }

            Optional<MojangSkinDataResult> optional = mojangAPI.getSkin(playerName);
            atomicAdapter.get().setCachedUUID(playerName,
                    MojangCacheData.of(optional.isPresent(),
                            optional.map(MojangSkinDataResult::getUniqueId).orElse(null),
                            Instant.now().getEpochSecond()));

            return optional;
        } catch (StorageAdapter.StorageException e) {
            logger.warning("Failed to get skin from cache for " + playerName, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<UUID> getUUID(String playerName, boolean allowExpired) throws DataRequestException {
        if (!C.validMojangUsername(playerName)) {
            return Optional.empty();
        }

        try {
            Optional<MojangCacheData> stored = getCachedData(playerName, allowExpired);
            if (stored.isPresent()) {
                return Optional.ofNullable(stored.get().getUniqueId());
            }

            try {
                Optional<UUID> uuid = mojangAPI.getUUID(playerName);

                atomicAdapter.get().setCachedUUID(playerName,
                        MojangCacheData.of(uuid.isPresent(), uuid.orElse(null), Instant.now().getEpochSecond()));

                return uuid;
            } catch (DataRequestException e) {
                logger.debug("Failed to get UUID from Mojang for " + playerName, e);

                throw new DataRequestExceptionShared(e);
            }
        } catch (StorageAdapter.StorageException e) {
            logger.warning("Failed to get UUID from cache for " + playerName, e);
            return Optional.empty();
        }
    }

    private Optional<MojangCacheData> getCachedData(String playerName, boolean allowExpired) throws StorageAdapter.StorageException {
        Optional<MojangCacheData> optional = atomicAdapter.get().getCachedUUID(playerName);

        if (optional.isPresent() && (allowExpired || isValidUUIDTimestamp(optional.get().getTimestamp()))) {
            return optional;
        }

        return Optional.empty();
    }

    private boolean isValidUUIDTimestamp(long epochSecond) {
        int expiresAfter = settings.getProperty(StorageConfig.UUID_EXPIRES_AFTER);
        return expiresAfter <= 0 || Instant.now().getEpochSecond() - epochSecond <= expiresAfter;
    }
}
