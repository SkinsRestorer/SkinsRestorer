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
package net.skinsrestorer.shared.storage.adapter;

import net.skinsrestorer.shared.storage.model.cache.MojangCacheData;
import net.skinsrestorer.shared.storage.model.player.PlayerData;
import net.skinsrestorer.shared.storage.model.skin.CustomSkinData;
import net.skinsrestorer.shared.storage.model.skin.PlayerSkinData;
import net.skinsrestorer.shared.storage.model.skin.URLSkinData;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface StorageAdapter {
    Optional<PlayerData> getPlayerData(UUID uuid) throws StorageException;

    void setPlayerData(UUID uuid, PlayerData data);

    Optional<PlayerSkinData> getPlayerSkinData(UUID uuid) throws StorageException;

    void removePlayerSkinData(UUID uuid);

    void setPlayerSkinData(UUID uuid, PlayerSkinData skinData);

    Optional<URLSkinData> getURLSkinData(String url) throws StorageException;

    void removeURLSkinData(String url);

    void setURLSkinData(String url, URLSkinData skinData);

    Optional<CustomSkinData> getCustomSkinData(String skinName) throws StorageException;

    void removeCustomSkinData(String skinName);

    void setCustomSkinData(String skinName, CustomSkinData skinData);

    Map<String, String> getStoredSkins(int offset); // TODO: Redesign this

    void purgeStoredOldSkins(long targetPurgeTimestamp) throws StorageException; // TODO: Redesign this

    Optional<MojangCacheData> getCachedUUID(String playerName) throws StorageException;

    void setCachedUUID(String playerName, MojangCacheData mojangCacheData);

    class StorageException extends Exception {
        public StorageException(Throwable cause) {
            super(cause);
        }
    }
}
