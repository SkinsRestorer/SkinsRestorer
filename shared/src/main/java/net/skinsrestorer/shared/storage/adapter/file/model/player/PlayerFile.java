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
