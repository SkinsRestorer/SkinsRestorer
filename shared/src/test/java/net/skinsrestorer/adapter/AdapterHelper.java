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
package net.skinsrestorer.adapter;

import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.property.SkinVariant;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.model.cache.MojangCacheData;
import net.skinsrestorer.shared.storage.model.player.PlayerData;
import net.skinsrestorer.shared.storage.model.skin.CustomSkinData;
import net.skinsrestorer.shared.storage.model.skin.PlayerSkinData;
import net.skinsrestorer.shared.storage.model.skin.URLSkinData;
import org.junit.Assert;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class AdapterHelper {
    private static final String DEFAULT_NAME = "Pistonmaster";
    private static final UUID DEFAULT_UUID = UUID.nameUUIDFromBytes(DEFAULT_NAME.getBytes(StandardCharsets.UTF_8));

    public static void testAdapter(StorageAdapter adapter, boolean checkCustom) {
        adapter.setCachedUUID("test", MojangCacheData.of(true, UUID.randomUUID(), -1));
        adapter.setPlayerData(UUID.randomUUID(), PlayerData.of(UUID.randomUUID(), null));
        adapter.setPlayerSkinData(DEFAULT_UUID, PlayerSkinData.of(DEFAULT_UUID, DEFAULT_NAME, SkinProperty.of("test", "test"), -1));
        adapter.setCustomSkinData("test-skin", CustomSkinData.of("test-skin", SkinProperty.of("test", "test")));
        adapter.setURLSkinData("test", URLSkinData.of("https://test.com", "test", SkinProperty.of("test", "test"), SkinVariant.CLASSIC));

        if (checkCustom) {
            // Check if offset works as well, we actually have two skins in the storage for GUI
            Assert.assertEquals(1, adapter.getStoredGUISkins(1).size());
        } else {
            Assert.assertEquals(1, adapter.getStoredGUISkins(0).size());
        }
    }
}
