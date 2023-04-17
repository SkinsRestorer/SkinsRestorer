package net.skinsrestorer.api.property;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(staticName = "of")
public class SkinIdentifier {
    @NonNull
    private final String identifier;
    @NonNull
    private final SkinType skinType;
}
