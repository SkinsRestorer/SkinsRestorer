/*
 * SkinsRestorer
 * Copyright (C) 2024  SkinsRestorer Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.shared.storage;

import ch.jalu.configme.SettingsManager;
import ch.jalu.injector.Injector;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.MojangSkinDataResult;
import net.skinsrestorer.api.storage.CacheStorage;
import net.skinsrestorer.api.storage.SkinStorage;
import net.skinsrestorer.shared.config.StorageConfig;
import net.skinsrestorer.shared.connections.MojangAPIImpl;
import net.skinsrestorer.shared.exception.DataRequestExceptionShared;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.storage.adapter.AdapterReference;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.model.cache.MojangCacheData;
import net.skinsrestorer.shared.utils.SRHelpers;
import net.skinsrestorer.shared.utils.ValidationUtil;

import javax.inject.Inject;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CacheStorageImpl implements CacheStorage {
    private final SRLogger logger;
    private final MojangAPIImpl mojangAPI;
    private final SettingsManager settings;
    private final AdapterReference adapterReference;
    private final Injector injector;

    @Override
    @Deprecated
    public Optional<MojangSkinDataResult> getSkin(String playerName, boolean allowExpired) throws DataRequestException {
        return injector.getSingleton(SkinStorage.class).getPlayerSkin(playerName, allowExpired);
    }

    @Override
    public Optional<UUID> getUUID(String playerName, boolean allowExpired) throws DataRequestException {
        if (ValidationUtil.invalidMinecraftUsername(playerName)) {
            return Optional.empty();
        }

        try {
            Optional<MojangCacheData> stored = getCachedData(playerName, allowExpired);
            if (stored.isPresent()) {
                return stored.get().getUniqueId();
            }

            try {
                Optional<UUID> uuid = mojangAPI.getUUID(playerName);

                adapterReference.get().setCachedUUID(playerName,
                        MojangCacheData.of(uuid.orElse(null), SRHelpers.getEpochSecond()));

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

    public Optional<MojangCacheData> getCachedData(String playerName, boolean allowExpired) throws StorageAdapter.StorageException {
        Optional<MojangCacheData> optional = adapterReference.get().getCachedUUID(playerName);

        if (optional.isPresent() && (allowExpired || isValidUUIDTimestamp(optional.get().getTimestamp()))) {
            return optional;
        }

        return Optional.empty();
    }

    private boolean isValidUUIDTimestamp(long epochSecond) {
        int expiresAfter = Math.max(settings.getProperty(StorageConfig.UUID_EXPIRES_AFTER), 5);
        return SRHelpers.getEpochSecond() - epochSecond <= expiresAfter;
    }
}
