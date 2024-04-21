package net.skinsrestorer.shared.connections.responses.uuid;

import net.skinsrestorer.shared.connections.responses.EclipseCacheData;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record EclipseUUIDResponse(EclipseCacheData cacheData, boolean exists, @Nullable UUID uuid) {
  }
