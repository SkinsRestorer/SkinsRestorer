package net.skinsrestorer.shared.storage.adapter.file.model.skin;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.storage.model.skin.URLSkinData;

@Getter
@NoArgsConstructor
public class URLSkinFile {
    private static final int CURRENT_DATA_VERSION = 1;
    private String url;
    private String mineSkinId;
    private String value;
    private String signature;
    private int dataVersion;

    public static URLSkinFile fromURLSkinData(URLSkinData urlSkinData) {
        URLSkinFile urlSkinFile = new URLSkinFile();
        urlSkinFile.url = urlSkinData.getUrl();
        urlSkinFile.mineSkinId = urlSkinData.getMineSkinId();
        urlSkinFile.value = urlSkinData.getProperty().getValue();
        urlSkinFile.signature = urlSkinData.getProperty().getSignature();
        urlSkinFile.dataVersion = CURRENT_DATA_VERSION;
        return urlSkinFile;
    }

    public URLSkinData toURLSkinData() {
        return URLSkinData.of(url, mineSkinId, SkinProperty.of(value, signature));
    }
}
