package net.skinsrestorer.shared.storage.adapter.file;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SkinStorageType {
    private String skinName;
    private String value;
    private String signature;
    private long timestamp;

    public boolean isInvalid() {
        return value == null || signature == null || timestamp <= 0;
    }
}
