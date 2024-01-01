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
package net.skinsrestorer.shared.storage.adapter.file.model.skin;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.storage.model.skin.PlayerSkinData;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class PlayerSkinFile {
    private static final int CURRENT_DATA_VERSION = 1;
    private UUID uniqueId;
    private String lastKnownName;
    private String value;
    private String signature;
    private long timestamp;
    private int dataVersion;

    public static PlayerSkinFile fromPlayerSkinData(PlayerSkinData playerSkinData) {
        PlayerSkinFile playerSkinFile = new PlayerSkinFile();
        playerSkinFile.uniqueId = playerSkinData.getUniqueId();
        playerSkinFile.lastKnownName = playerSkinData.getLastKnownName();
        playerSkinFile.value = playerSkinData.getProperty().getValue();
        playerSkinFile.signature = playerSkinData.getProperty().getSignature();
        playerSkinFile.timestamp = playerSkinData.getTimestamp();
        playerSkinFile.dataVersion = CURRENT_DATA_VERSION;
        return playerSkinFile;
    }

    public PlayerSkinData toPlayerSkinData() {
        return PlayerSkinData.of(uniqueId, lastKnownName, SkinProperty.of(value, signature), timestamp);
    }
}
