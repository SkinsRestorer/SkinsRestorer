package net.skinsrestorer.shared.storage.adapter.file.model.skin;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.storage.model.skin.CustomSkinData;

@Getter
@NoArgsConstructor
public class CustomSkinFile {
    private static final int CURRENT_DATA_VERSION = 1;
    private String skinName;
    private String value;
    private String signature;
    private int dataVersion;

    public static CustomSkinFile fromCustomSkinData(CustomSkinData customSkinData) {
        CustomSkinFile customSkinFile = new CustomSkinFile();
        customSkinFile.skinName = customSkinData.getSkinName();
        customSkinFile.value = customSkinData.getProperty().getValue();
        customSkinFile.signature = customSkinData.getProperty().getSignature();
        customSkinFile.dataVersion = CURRENT_DATA_VERSION;
        return customSkinFile;
    }

    public CustomSkinData toCustomSkinData() {
        return CustomSkinData.of(skinName, SkinProperty.of(value, signature));
    }
}
