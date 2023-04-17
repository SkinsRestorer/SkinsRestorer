package net.skinsrestorer.shared.storage.model.skin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;

import java.util.UUID;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class PlayerSkinData {
    private final UUID uniqueId;
    private final SkinProperty property;
    private final long timestampSeconds;
}
