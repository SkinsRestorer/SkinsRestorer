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
package net.skinsrestorer.adapter;

import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinVariant;
import net.skinsrestorer.shared.storage.HardcodedSkins;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.model.cache.MojangCacheData;
import net.skinsrestorer.shared.storage.model.player.FavouriteData;
import net.skinsrestorer.shared.storage.model.player.HistoryData;
import net.skinsrestorer.shared.storage.model.player.PlayerData;
import net.skinsrestorer.shared.storage.model.skin.CustomSkinData;
import net.skinsrestorer.shared.storage.model.skin.PlayerSkinData;
import net.skinsrestorer.shared.storage.model.skin.URLSkinData;
import org.junit.jupiter.api.Assertions;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class AdapterHelper {
    private static final String DEFAULT_NAME = "Pistonmaster";
    private static final UUID DEFAULT_UUID = UUID.nameUUIDFromBytes(DEFAULT_NAME.getBytes(StandardCharsets.UTF_8));

    public static void testAdapter(StorageAdapter adapter) {
        UUID playerId = UUID.randomUUID();
        PlayerData playerData = PlayerData.of(playerId, null, List.of(
                HistoryData.of(0, SkinIdentifier.ofCustom("abc"))
        ), List.of(
                FavouriteData.of(0, SkinIdentifier.ofCustom("abc"))
        ));

        adapter.setCachedUUID("test", MojangCacheData.of(UUID.randomUUID(), -1));
        adapter.setPlayerData(playerId, playerData);
        adapter.setPlayerSkinData(DEFAULT_UUID, PlayerSkinData.of(DEFAULT_UUID, DEFAULT_NAME,
                HardcodedSkins.STEVE.getProperty(), -1));
        adapter.setURLSkinData("test", URLSkinData.of("https://test.com", "test",
                HardcodedSkins.STEVE.getProperty(), SkinVariant.CLASSIC));
        adapter.setCustomSkinData("test-skin", CustomSkinData.of("test-skin",
                null, HardcodedSkins.STEVE.getProperty()));
        adapter.setCustomSkinData("test-skin2", CustomSkinData.of("test-skin2",
                null, HardcodedSkins.ALEX.getProperty()));

        Assertions.assertEquals(2, adapter.getTotalCustomSkins());
        Assertions.assertEquals(2, adapter.getCustomGUISkins(0, Integer.MAX_VALUE).size());

        // Check if offset works as well, we actually have two skins in the storage for GUI
        Assertions.assertEquals(1, adapter.getCustomGUISkins(1, Integer.MAX_VALUE).size());

        Assertions.assertEquals(1, adapter.getTotalPlayerSkins());
        Assertions.assertEquals(1, adapter.getPlayerGUISkins(0, Integer.MAX_VALUE).size());

        // Check if offset works as well, we actually have one skins in the storage for GUI
        Assertions.assertEquals(0, adapter.getPlayerGUISkins(1, Integer.MAX_VALUE).size());

        try {
            Assertions.assertEquals(playerData, adapter.getPlayerData(playerId).orElseThrow());
        } catch (StorageAdapter.StorageException e) {
            throw new RuntimeException(e);
        }
    }
}
