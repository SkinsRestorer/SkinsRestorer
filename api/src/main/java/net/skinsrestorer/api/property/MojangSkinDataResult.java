package net.skinsrestorer.api.property;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class MojangSkinDataResult {
    private final UUID uniqueId;
    private final SkinProperty skinProperty;
}
