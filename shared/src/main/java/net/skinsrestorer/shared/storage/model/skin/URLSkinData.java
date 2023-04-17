package net.skinsrestorer.shared.storage.model.skin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class URLSkinData {
    private final String url;
    private final String mineSkinId;
    private final SkinProperty property;
}
