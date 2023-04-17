/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
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
package net.skinsrestorer.shared.storage.adapter.file.model.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinType;
import net.skinsrestorer.shared.storage.model.player.PlayerData;

@Getter
@NoArgsConstructor
public class PlayerFile {
    private static final int CURRENT_DATA_VERSION = 1;
    private IdentifierFile skinIdentifier;
    private int dataVersion;

    public static PlayerFile fromPlayerData(PlayerData playerData) {
        PlayerFile playerFile = new PlayerFile();
        playerFile.skinIdentifier = IdentifierFile.of(playerData.getSkinIdentifier());
        playerFile.dataVersion = CURRENT_DATA_VERSION;
        return playerFile;
    }

    public PlayerData toPlayerData() {
        return PlayerData.of(uniqueId, skinIdentifier == null ? null : skinIdentifier.toIdentifier());
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    public static class IdentifierFile {
        private String identifier;
        private SkinType type;

        public static IdentifierFile of(SkinIdentifier identifier) {
            if (identifier == null) {
                return null;
            }

            return new IdentifierFile(identifier.getIdentifier(), identifier.getSkinType());
        }

        public SkinIdentifier toIdentifier() {
            return SkinIdentifier.of(identifier, type);
        }
    }
}
