package net.skinsrestorer.shared.storage.model.cache;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class MojangCacheData {
    private final UUID uniqueId;
    private final String lastKnownName;
    private final long timestampSeconds; // TODO: Change to Instant/Seconds
}
