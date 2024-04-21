package net.skinsrestorer.shared.connections.responses;

public record EclipseCacheData(
        CacheState state,
        long createdAt
) {
    public enum CacheState {
        HIT,
        MISS
    }
}
