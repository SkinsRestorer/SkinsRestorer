/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.shared.skinsafety;

import ch.jalu.configme.SettingsManager;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.property.SkinType;
import net.skinsrestorer.scissors.skin.SkinHashing;
import net.skinsrestorer.shared.config.SkinSafetyConfig;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.storage.SkinInput;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.permissions.Permission;
import net.skinsrestorer.shared.utils.SRHelpers;
import net.skinsrestorer.shared.utils.ValidationUtil;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SkinSafetyService {
    private static final int IMAGE_TIMEOUT_MS = 15_000;
    private static final int MAX_IMAGE_BYTES = 4 * 1024 * 1024;

    private final SettingsManager settings;
    private final SRLogger logger;
    private final SkinSafetyState state;
    private final ConcurrentMap<String, Optional<SkinSafetyImageDigests>> imageDigestCache = new ConcurrentHashMap<>();

    @Inject
    public SkinSafetyService(SettingsManager settings, SRLogger logger, SkinSafetyState state) {
        this.settings = settings;
        this.logger = logger;
        this.state = state;
    }

    public boolean isEnabled() {
        return settings.getProperty(SkinSafetyConfig.ENABLED);
    }

    public SkinSafetyMode getMode() {
        return settings.getProperty(SkinSafetyConfig.MODE);
    }

    public String getBypassPermission() {
        return settings.getProperty(SkinSafetyConfig.BYPASS_PERMISSION);
    }

    public boolean hasBypass(@Nullable SRCommandSender sender) {
        return sender != null && sender.hasPermission(Permission.of(getBypassPermission()));
    }

    public SkinSafetyDecision checkRequestedInput(String input) {
        String sanitizedInput = SRHelpers.sanitizeSkinInput(input);
        SkinInput parsed = SkinInput.parse(sanitizedInput);
        String normalizedInput = normalize(parsed.name());
        String sourceUrl = ValidationUtil.validSkinUrl(parsed.name()) ? parsed.name() : null;

        return evaluate(
                normalizedInput,
                null,
                null,
                sourceUrl,
                sourceUrl,
                null,
                sourceUrl
        );
    }

    public SkinSafetyDecision checkResolved(String input, @Nullable SkinIdentifier identifier, @Nullable SkinProperty property) {
        String sanitizedInput = SRHelpers.sanitizeSkinInput(input);
        SkinInput parsed = SkinInput.parse(sanitizedInput);

        UUID playerUuid = identifier != null && identifier.getSkinType() == SkinType.PLAYER ? identifier.getPlayerUniqueId() : null;
        String playerName = identifier != null && identifier.getSkinType() == SkinType.PLAYER ? normalize(parsed.name()) : null;
        String sourceUrl = ValidationUtil.validSkinUrl(parsed.name()) ? parsed.name() : null;
        String textureHash = safeTextureHash(property);
        String imageUrl = property != null && !property.getValue().isEmpty() ? safeTextureUrl(property) : sourceUrl;

        return evaluate(
                normalize(parsed.name()),
                playerUuid,
                playerName,
                sourceUrl,
                imageUrl,
                textureHash,
                textureHash != null ? textureHash : imageUrl
        );
    }

    public SkinSafetyDecision checkPlayerSkin(UUID playerUuid, String playerName, SkinProperty property) {
        String textureHash = safeTextureHash(property);
        String imageUrl = safeTextureUrl(property);

        return evaluate(
                normalize(playerName),
                playerUuid,
                normalize(playerName),
                null,
                imageUrl,
                textureHash,
                textureHash != null ? textureHash : imageUrl
        );
    }

    public SkinSafetyDecision checkRecommendation(String recommendationId, SkinProperty property) {
        String textureHash = safeTextureHash(property);
        String imageUrl = safeTextureUrl(property);

        return evaluate(
                normalize(recommendationId),
                null,
                null,
                null,
                imageUrl,
                textureHash,
                textureHash != null ? textureHash : imageUrl
        );
    }

    public void logMatch(String context, SkinSafetyDecision decision) {
        if (!decision.matched()) {
            return;
        }

        SkinSafetyMatch match = decision.match();
        logger.warning("[SkinSafety] %s matched %s '%s' from %s"
                .formatted(context, match.matchType(), match.matchedValue(), match.source()));
    }

    private SkinSafetyDecision evaluate(@Nullable String normalizedName,
                                        @Nullable UUID playerUuid,
                                        @Nullable String playerName,
                                        @Nullable String sourceUrl,
                                        @Nullable String imageUrl,
                                        @Nullable String textureHash,
                                        @Nullable String imageCacheKey) {
        SkinSafetyMode mode = getMode();
        if (!isEnabled()) {
            return SkinSafetyDecision.allow(mode);
        }

        SkinSafetyBlocklistSnapshot snapshot = state.getSnapshot();

        if (textureHash != null && snapshot.allowTextureHashes().contains(normalize(textureHash))) {
            return SkinSafetyDecision.allow(mode);
        }
        if (playerUuid != null && snapshot.allowPlayerUuids().contains(playerUuid)) {
            return SkinSafetyDecision.allow(mode);
        }

        if (normalizedName != null && snapshot.blockedNames().contains(normalizedName)) {
            return new SkinSafetyDecision(mode, new SkinSafetyMatch(SkinSafetyMatchType.NAME, normalizedName, "blocklist"));
        }
        if (textureHash != null && snapshot.blockedTextureHashes().contains(normalize(textureHash))) {
            return new SkinSafetyDecision(mode, new SkinSafetyMatch(SkinSafetyMatchType.TEXTURE_HASH, normalize(textureHash), "blocklist"));
        }
        if (playerUuid != null && snapshot.blockedPlayerUuids().contains(playerUuid)) {
            return new SkinSafetyDecision(mode, new SkinSafetyMatch(SkinSafetyMatchType.PLAYER_UUID, playerUuid.toString(), "blocklist"));
        }
        if (playerName != null && snapshot.blockedPlayerNames().contains(playerName)) {
            return new SkinSafetyDecision(mode, new SkinSafetyMatch(SkinSafetyMatchType.PLAYER_NAME, playerName, "blocklist"));
        }
        if (sourceUrl != null) {
            String normalizedUrl = normalize(sourceUrl);
            for (String blockedPrefix : snapshot.blockedUrlPrefixes()) {
                if (normalizedUrl.startsWith(blockedPrefix)) {
                    return new SkinSafetyDecision(mode, new SkinSafetyMatch(SkinSafetyMatchType.URL_PREFIX, blockedPrefix, "blocklist"));
                }
            }

            String domain = getDomain(normalizedUrl);
            if (domain != null) {
                for (String blockedDomain : snapshot.blockedDomains()) {
                    if (domain.equals(blockedDomain) || domain.endsWith("." + blockedDomain)) {
                        return new SkinSafetyDecision(mode, new SkinSafetyMatch(SkinSafetyMatchType.DOMAIN, blockedDomain, "blocklist"));
                    }
                }
            }
        }

        if (snapshot.hasDigestRules() && imageUrl != null) {
            Optional<SkinSafetyImageDigests> imageDigests = loadImageDigests(imageCacheKey == null ? imageUrl : imageCacheKey, imageUrl);
            if (imageDigests.isPresent()) {
                String matchedDigest = findMatchingDigest(snapshot, imageDigests.get(), SkinSafetyDigestAlgorithm.SHA256);
                if (matchedDigest != null) {
                    return new SkinSafetyDecision(mode, new SkinSafetyMatch(
                            SkinSafetyMatchType.DIGEST,
                            SkinSafetyDigestAlgorithm.SHA256.encode(matchedDigest),
                            imageUrl
                    ));
                }

                matchedDigest = findMatchingDigest(snapshot, imageDigests.get(), SkinSafetyDigestAlgorithm.PERCEPTUAL_V1);
                if (matchedDigest != null) {
                    return new SkinSafetyDecision(mode, new SkinSafetyMatch(
                            SkinSafetyMatchType.DIGEST,
                            SkinSafetyDigestAlgorithm.PERCEPTUAL_V1.encode(matchedDigest),
                            imageUrl
                    ));
                }
            }
        }

        return SkinSafetyDecision.allow(mode);
    }

    private String findMatchingDigest(SkinSafetyBlocklistSnapshot snapshot,
                                      SkinSafetyImageDigests imageDigests,
                                      SkinSafetyDigestAlgorithm algorithm) {
        String candidateDigest = imageDigests.getDigest(algorithm);
        if (candidateDigest == null) {
            return null;
        }

        int perceptualHashMaxDistance = settings.getProperty(SkinSafetyConfig.PERCEPTUAL_HASH_MAX_DISTANCE);
        for (String blockedDigest : snapshot.blockedDigests(algorithm)) {
            if (algorithm.matches(candidateDigest, blockedDigest, perceptualHashMaxDistance)) {
                return blockedDigest;
            }
        }
        return null;
    }

    private Optional<SkinSafetyImageDigests> loadImageDigests(String cacheKey, String imageUrl) {
        return imageDigestCache.computeIfAbsent(cacheKey, ignored -> {
            try {
                byte[] bytes = readImageBytes(imageUrl);
                if (bytes.length == 0) {
                    return Optional.empty();
                }

                var image = ImageIO.read(new ByteArrayInputStream(bytes));
                if (image == null) {
                    return Optional.empty();
                }

                SkinHashing.SkinFingerprint fingerprint = SkinHashing.fingerprint(image);
                return Optional.of(SkinSafetyImageDigests.of(fingerprint.sha256(), fingerprint.perceptualHash()));
            } catch (Exception e) {
                logger.debug("Failed to hash skin image from %s".formatted(imageUrl), e);
                return Optional.empty();
            }
        });
    }

    private byte[] readImageBytes(String imageUrl) throws IOException {
        String dataPrefix = "data:image/png;base64,";
        if (imageUrl.startsWith(dataPrefix)) {
            return Base64.getDecoder().decode(imageUrl.substring(dataPrefix.length()));
        }

        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(IMAGE_TIMEOUT_MS);
        connection.setReadTimeout(IMAGE_TIMEOUT_MS);
        connection.setRequestMethod("GET");
        connection.setUseCaches(false);

        try (InputStream inputStream = connection.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            int totalRead = 0;
            while ((read = inputStream.read(buffer)) != -1) {
                totalRead += read;
                if (totalRead > MAX_IMAGE_BYTES) {
                    throw new IOException("Image exceeds maximum allowed size");
                }
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        }
    }

    private String safeTextureHash(@Nullable SkinProperty property) {
        if (property == null || property.getValue().isEmpty()) {
            return null;
        }

        try {
            return normalize(PropertyUtils.getSkinTextureHash(property));
        } catch (Exception e) {
            logger.debug("Failed to extract texture hash from property", e);
            return null;
        }
    }

    private String safeTextureUrl(@Nullable SkinProperty property) {
        if (property == null || property.getValue().isEmpty()) {
            return null;
        }

        try {
            return PropertyUtils.getSkinTextureUrl(property);
        } catch (Exception e) {
            logger.debug("Failed to extract texture URL from property", e);
            return null;
        }
    }

    private String getDomain(String url) {
        Optional<URL> parsed = SRHelpers.parseURL(url);
        return parsed.map(URL::getHost)
                .map(this::normalize)
                .orElse(null);
    }

    private String normalize(@Nullable String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
