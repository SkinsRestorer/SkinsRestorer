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
    private String value;
    private String signature;
    private long timestampSeconds; // TODO: Change to Instant/Seconds
    private int dataVersion;

    public static PlayerSkinFile fromPlayerSkinData(PlayerSkinData playerSkinData) {
        PlayerSkinFile playerSkinFile = new PlayerSkinFile();
        playerSkinFile.uniqueId = playerSkinData.getUniqueId();
        playerSkinFile.value = playerSkinData.getProperty().getValue();
        playerSkinFile.signature = playerSkinData.getProperty().getValue();
        playerSkinFile.timestampSeconds = playerSkinData.getTimestampSeconds();
        playerSkinFile.dataVersion = CURRENT_DATA_VERSION;
        return playerSkinFile;
    }

    public PlayerSkinData toPlayerSkinData() {
        return PlayerSkinData.of(uniqueId, SkinProperty.of(value, signature), timestampSeconds);
    }
}
