package net.skinsrestorer.shared.storage.model.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.skinsrestorer.api.property.SkinIdentifier;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Data
@AllArgsConstructor(staticName = "of")
public class PlayerData {
    private final UUID uniqueId;
    @Nullable
    private SkinIdentifier skinIdentifier;
}
