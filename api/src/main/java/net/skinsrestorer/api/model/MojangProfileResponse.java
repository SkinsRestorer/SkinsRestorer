package net.skinsrestorer.api.model;

import lombok.Data;

@Data
public class MojangProfileResponse {
    private long timestamp;
    private String profileId;
    private String profileName;
    private boolean signatureRequired;
    private MojangProfileTextures textures;
}
