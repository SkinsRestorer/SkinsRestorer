package net.skinsrestorer.shared.storage.model.skin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class CustomSkinData {
    private final String skinName;
    private final SkinProperty property;
}
