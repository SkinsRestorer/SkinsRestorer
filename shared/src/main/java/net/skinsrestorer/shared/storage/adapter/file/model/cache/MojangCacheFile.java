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
package net.skinsrestorer.shared.storage.adapter.file.model.cache;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.skinsrestorer.shared.storage.model.cache.MojangCacheData;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class MojangCacheFile {
    private static final int CURRENT_DATA_VERSION = 1;
    private boolean isPremium;
    private UUID uniqueId;
    private long timestamp;
    private int dataVersion;

    public static MojangCacheFile fromMojangCacheData(MojangCacheData cacheData) {
        MojangCacheFile mojangCacheFile = new MojangCacheFile();
        mojangCacheFile.isPremium = cacheData.isPremium();
        mojangCacheFile.uniqueId = cacheData.getUniqueId();
        mojangCacheFile.timestamp = cacheData.getTimestamp();
        mojangCacheFile.dataVersion = CURRENT_DATA_VERSION;
        return mojangCacheFile;
    }

    public MojangCacheData toCacheData() {
        return MojangCacheData.of(isPremium, uniqueId, timestamp);
    }
}
