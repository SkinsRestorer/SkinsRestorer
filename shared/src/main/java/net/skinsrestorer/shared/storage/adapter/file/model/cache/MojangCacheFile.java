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
package net.skinsrestorer.shared.storage.adapter.file.model.cache;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.skinsrestorer.shared.storage.model.cache.MojangCacheData;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class MojangCacheFile {
    private static final int CURRENT_DATA_VERSION = 1;
    private UUID uniqueId;
    private long timestamp;
    private int dataVersion;

    public static MojangCacheFile fromMojangCacheData(MojangCacheData cacheData) {
        MojangCacheFile mojangCacheFile = new MojangCacheFile();
        mojangCacheFile.uniqueId = cacheData.getUniqueId().orElse(null);
        mojangCacheFile.timestamp = cacheData.getTimestamp();
        mojangCacheFile.dataVersion = CURRENT_DATA_VERSION;
        return mojangCacheFile;
    }

    public MojangCacheData toCacheData() {
        return MojangCacheData.of(uniqueId, timestamp);
    }
}
