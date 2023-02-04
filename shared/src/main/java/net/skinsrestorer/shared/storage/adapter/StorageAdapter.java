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
package net.skinsrestorer.shared.storage.adapter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

public interface StorageAdapter {
    Optional<String> getStoredSkinNameOfPlayer(String playerName);

    void removeStoredSkinNameOfPlayer(String playerName);

    void setStoredSkinNameOfPlayer(String playerName, String skinName);

    Optional<StoredProperty> getStoredSkinData(String skinName) throws Exception;

    void removeStoredSkinData(String skinName);

    void setStoredSkinData(String skinName, StoredProperty storedProperty);

    Map<String, String> getStoredSkins(int offset);

    Optional<Long> getStoredTimestamp(String skinName);

    void purgeStoredOldSkins(long targetPurgeTimestamp) throws StorageException;

    @RequiredArgsConstructor
    @Getter
    class StoredProperty {
        private final String value;
        private final String signature;
        private final long timestamp;
    }

    class StorageException extends Exception {
        public StorageException(Throwable cause) {
            super(cause);
        }
    }
}
