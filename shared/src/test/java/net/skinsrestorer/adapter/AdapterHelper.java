package net.skinsrestorer.adapter;

import net.skinsrestorer.api.model.SkinVariant;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.model.cache.MojangCacheData;
import net.skinsrestorer.shared.storage.model.player.PlayerData;
import net.skinsrestorer.shared.storage.model.skin.PlayerSkinData;
import net.skinsrestorer.shared.storage.model.skin.URLSkinData;

import java.util.UUID;

public class AdapterHelper {
    public static void testAdapter(StorageAdapter adapter) {
        adapter.setCachedUUID("test", MojangCacheData.of(true, UUID.randomUUID(), -1));
        adapter.setPlayerData(UUID.randomUUID(), PlayerData.of(UUID.randomUUID(), null));
        adapter.setPlayerSkinData(UUID.randomUUID(), PlayerSkinData.of(UUID.randomUUID(), SkinProperty.of("test", "test"), -1));
        adapter.setURLSkinData("test", URLSkinData.of("https://test.com", "test", SkinProperty.of("test", "test"), SkinVariant.CLASSIC));
    }
}
