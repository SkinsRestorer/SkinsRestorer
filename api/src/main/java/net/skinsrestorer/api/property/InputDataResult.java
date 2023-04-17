package net.skinsrestorer.api.property;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class InputDataResult {
    private final SkinIdentifier identifier;
    private final SkinProperty property;
}
