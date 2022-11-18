package net.skinsrestorer.shared.storage.adapter.file;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayerStorageType {
    private String skinName;

    public boolean isInvalid() {
        return skinName == null;
    }
}
