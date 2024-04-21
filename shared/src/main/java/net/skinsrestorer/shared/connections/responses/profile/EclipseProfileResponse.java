package net.skinsrestorer.shared.connections.responses.profile;

import net.skinsrestorer.shared.connections.responses.EclipseCacheData;
import org.jetbrains.annotations.Nullable;

public record EclipseProfileResponse(EclipseCacheData cacheData, boolean exists, @Nullable SkinProperty skinProperty) {
    public record SkinProperty(
            String value,
            String signature
    ) {
    }
}
