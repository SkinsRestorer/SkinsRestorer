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
    private String lastKnownName;
    private long timestampSeconds;
    private int dataVersion;

    public static MojangCacheFile fromMojangCacheData(MojangCacheData cacheData) {
        MojangCacheFile mojangCacheFile = new MojangCacheFile();
        mojangCacheFile.uniqueId = cacheData.getUniqueId();
        mojangCacheFile.lastKnownName = cacheData.getLastKnownName();
        mojangCacheFile.timestampSeconds = cacheData.getTimestampSeconds();
        mojangCacheFile.dataVersion = CURRENT_DATA_VERSION;
        return mojangCacheFile;
    }

    public MojangCacheData toCacheData() {
        return MojangCacheData.of(uniqueId, lastKnownName, timestampSeconds);
    }
}
