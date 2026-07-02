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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.InputDataResult;
import net.skinsrestorer.scissors.skin.SkinHashing;
import net.skinsrestorer.shared.config.CommandConfig;
import net.skinsrestorer.shared.config.SkinSafetyConfig;
import net.skinsrestorer.shared.connections.http.HttpClient;
import net.skinsrestorer.shared.connections.http.HttpResponse;
import net.skinsrestorer.shared.connections.responses.SkinSafetyBlocklistResponse;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.storage.HardcodedSkins;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.utils.SRHelpers;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinSafetyState {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String BLOCKED_PNG_FOLDER = "blocked";
    private static final long SCHEDULER_INTERVAL_SECONDS = TimeUnit.MINUTES.toSeconds(5);

    private final SRPlugin plugin;
    private final SRPlatformAdapter adapter;
    private final SRLogger logger;
    private final HttpClient httpClient;
    private final SettingsManager settings;
    private final AtomicBoolean scheduled = new AtomicBoolean();
    private final AtomicLong lastRefreshAtMillis = new AtomicLong();
    private volatile SkinSafetyBlocklistSnapshot snapshot = SkinSafetyBlocklistSnapshot.empty();

    public void scheduleRefresh() {
        refreshNow();

        if (!scheduled.compareAndSet(false, true)) {
            return;
        }

        adapter.runRepeatAsync(this::refreshIfDue, SCHEDULER_INTERVAL_SECONDS, SCHEDULER_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public void refreshNow() {
        if (!settings.getProperty(SkinSafetyConfig.ENABLED)) {
            snapshot = SkinSafetyBlocklistSnapshot.empty();
            lastRefreshAtMillis.set(System.currentTimeMillis());
            return;
        }

        try {
            Files.createDirectories(getSkinSafetyFolder());
            Files.createDirectories(getBlockedPngFolder());
        } catch (IOException e) {
            logger.warning("Failed to create skin safety directories: %s".formatted(e.getMessage()));
        }

        SkinSafetyBlocklistResponse remote = loadRemoteBlocklist();
        snapshot = mergeSnapshot(remote);
        lastRefreshAtMillis.set(System.currentTimeMillis());
    }

    private void refreshIfDue() {
        if (!settings.getProperty(SkinSafetyConfig.ENABLED)) {
            return;
        }

        long refreshIntervalMillis = TimeUnit.MINUTES.toMillis(settings.getProperty(SkinSafetyConfig.PUBLIC_BLOCKLIST_ENABLED)
                ? settings.getProperty(SkinSafetyConfig.PUBLIC_BLOCKLIST_REFRESH_MINUTES)
                : 5);
        long lastRefresh = lastRefreshAtMillis.get();
        long now = System.currentTimeMillis();
        if (lastRefresh != 0 && now - lastRefresh < refreshIntervalMillis) {
            return;
        }

        refreshNow();
    }

    public SkinSafetyBlocklistSnapshot getSnapshot() {
        return snapshot;
    }

    public Path getSkinSafetyFolder() {
        return plugin.getDataFolder().resolve("skin-safety");
    }

    public Path getBlockedPngFolder() {
        return getSkinSafetyFolder().resolve(BLOCKED_PNG_FOLDER);
    }

    public Path getCachedBlocklistPath() {
        return getSkinSafetyFolder().resolve("blocklist-cache.json");
    }

    public InputDataResult resolveFallbackSkin(SkinStorageImpl skinStorage, SkinSafetyService skinSafetyService) {
        String fallbackInput = settings.getProperty(SkinSafetyConfig.FALLBACK_SKIN).trim();
        if (!fallbackInput.isEmpty()) {
            try {
                Optional<InputDataResult> fallback = skinStorage.findOrCreateSkinData(fallbackInput, null, false);
                if (fallback.isPresent() && !skinSafetyService.checkResolved(fallbackInput, fallback.get().getIdentifier(), fallback.get().getProperty()).shouldBlock()) {
                    return fallback.get();
                }
            } catch (Exception e) {
                logger.warning("Failed to resolve configured skin safety fallback '%s': %s".formatted(fallbackInput, e.getMessage()));
            }
        }

        return HardcodedSkins.STEVE;
    }

    private @Nullable SkinSafetyBlocklistResponse loadRemoteBlocklist() {
        SkinSafetyBlocklistResponse cachedCopy = loadCachedBlocklist();
        if (!settings.getProperty(SkinSafetyConfig.PUBLIC_BLOCKLIST_ENABLED)) {
            return cachedCopy;
        }

        String url = settings.getProperty(SkinSafetyConfig.PUBLIC_BLOCKLIST_URL).trim();
        if (url.isEmpty()) {
            return cachedCopy;
        }

        try {
            HttpResponse response = httpClient.execute(
                    java.net.URI.create(url),
                    null,
                    HttpClient.HttpType.JSON,
                    plugin.getUserAgent(),
                    HttpClient.HttpMethod.GET,
                    Map.of(),
                    20_000
            );
            if (response.statusCode() != 200) {
                logger.warning("Skin Safety blocklist request failed with status %d".formatted(response.statusCode()));
                return cachedCopy;
            }

            SkinSafetyBlocklistResponse remote = response.getBodyAs(SkinSafetyBlocklistResponse.class);
            if (remote == null) {
                logger.warning("Skin Safety blocklist returned an empty response.");
                return cachedCopy;
            }

            try {
                SRHelpers.writeIfNeeded(getCachedBlocklistPath(), GSON.toJson(remote));
            } catch (IOException e) {
                logger.warning("Failed to write cached skin safety blocklist: %s".formatted(e.getMessage()));
            }
            return remote;
        } catch (Exception e) {
            logger.warning("Failed to refresh public skin safety blocklist: %s".formatted(e.getMessage()));
            return cachedCopy;
        }
    }

    private @Nullable SkinSafetyBlocklistResponse loadCachedBlocklist() {
        if (!settings.getProperty(SkinSafetyConfig.PUBLIC_BLOCKLIST_USE_CACHED_COPY)) {
            return null;
        }

        Path path = getCachedBlocklistPath();
        if (!Files.exists(path)) {
            return null;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, SkinSafetyBlocklistResponse.class);
        } catch (IOException e) {
            logger.warning("Failed to load cached skin safety blocklist: %s".formatted(e.getMessage()));
            return null;
        }
    }

    private SkinSafetyBlocklistSnapshot mergeSnapshot(@Nullable SkinSafetyBlocklistResponse remote) {
        Set<String> blockedNames = normalizeStrings(settings.getProperty(SkinSafetyConfig.LOCAL_BLOCKED_NAMES));
        Set<String> blockedTextureHashes = normalizeStrings(settings.getProperty(SkinSafetyConfig.LOCAL_BLOCKED_TEXTURE_HASHES));
        Set<UUID> blockedPlayerUuids = parseUuids(settings.getProperty(SkinSafetyConfig.LOCAL_BLOCKED_PLAYER_UUIDS));
        Set<String> blockedPlayerNames = normalizeStrings(settings.getProperty(SkinSafetyConfig.LOCAL_BLOCKED_PLAYER_NAMES));
        Set<String> blockedUrlPrefixes = normalizeStrings(settings.getProperty(SkinSafetyConfig.LOCAL_BLOCKED_URL_PREFIXES));
        Set<String> blockedDomains = normalizeStrings(settings.getProperty(SkinSafetyConfig.LOCAL_BLOCKED_DOMAINS));
        Map<SkinSafetyDigestAlgorithm, Set<String>> blockedDigests = parseDigests(settings.getProperty(SkinSafetyConfig.LOCAL_BLOCKED_DIGESTS));
        Set<String> allowTextureHashes = normalizeStrings(settings.getProperty(SkinSafetyConfig.LOCAL_ALLOWED_TEXTURE_HASHES));
        Set<UUID> allowPlayerUuids = parseUuids(settings.getProperty(SkinSafetyConfig.LOCAL_ALLOWED_PLAYER_UUIDS));

        if (settings.getProperty(CommandConfig.DISABLED_SKINS_ENABLED)) {
            blockedNames.addAll(normalizeStrings(settings.getProperty(CommandConfig.DISABLED_SKINS)));
        }

        loadBlockedPngFingerprints(blockedDigests);

        if (remote != null && remote.getEntries() != null) {
            for (SkinSafetyBlocklistResponse.Entry entry : remote.getEntries()) {
                if (entry == null || entry.getMatchType() == null) {
                    continue;
                }
                if (!isRemoteEntryEnabled(entry)) {
                    continue;
                }

                switch (entry.getMatchType()) {
                    case NAME -> addNormalizedValue(blockedNames, entry.getValue());
                    case TEXTURE_HASH -> addNormalizedValue(blockedTextureHashes, entry.getValue());
                    case PLAYER_UUID -> parseUuid(entry.getValue()).ifPresent(blockedPlayerUuids::add);
                    case PLAYER_NAME -> addNormalizedValue(blockedPlayerNames, entry.getValue());
                    case URL_PREFIX -> addNormalizedValue(blockedUrlPrefixes, entry.getValue());
                    case DOMAIN -> addNormalizedValue(blockedDomains, entry.getValue());
                    case DIGEST -> parseRemoteDigest(entry.getDigestOrValue()).ifPresent(digest -> addDigest(blockedDigests, digest));
                    case PNG_SHA256 -> parseLegacyDigest(entry.getValue(), SkinSafetyDigestAlgorithm.SHA256)
                            .ifPresent(digest -> addDigest(blockedDigests, digest));
                    case PERCEPTUAL_HASH -> parseLegacyDigest(entry.getValue(), SkinSafetyDigestAlgorithm.PERCEPTUAL_V1)
                            .ifPresent(digest -> addDigest(blockedDigests, digest));
                }
            }
        }

        if (remote != null && remote.getAllow() != null) {
            if (remote.getAllow().getTextureHashes() != null) {
                allowTextureHashes.addAll(normalizeStrings(Arrays.asList(remote.getAllow().getTextureHashes())));
            }
            if (remote.getAllow().getPlayerUuids() != null) {
                allowPlayerUuids.addAll(parseUuids(Arrays.asList(remote.getAllow().getPlayerUuids())));
            }
        }

        return new SkinSafetyBlocklistSnapshot(
                blockedNames,
                blockedTextureHashes,
                blockedPlayerUuids,
                blockedPlayerNames,
                blockedUrlPrefixes,
                blockedDomains,
                blockedDigests,
                allowTextureHashes,
                allowPlayerUuids
        );
    }

    private void loadBlockedPngFingerprints(Map<SkinSafetyDigestAlgorithm, Set<String>> blockedDigests) {
        try (var files = Files.list(getBlockedPngFolder())) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .forEach(path -> {
                        try {
                            var image = ImageIO.read(path.toFile());
                            if (image == null) {
                                logger.warning("Skin Safety PNG '%s' could not be read as an image.".formatted(path.getFileName()));
                                return;
                            }

                            SkinHashing.SkinFingerprint fingerprint = SkinHashing.fingerprint(image);
                            addDigest(blockedDigests, SkinSafetyDigest.sha256(fingerprint.sha256()));
                            addDigest(blockedDigests, SkinSafetyDigest.perceptualV1(fingerprint.perceptualHash()));
                        } catch (Exception e) {
                            logger.warning("Failed to fingerprint blocked skin PNG '%s': %s".formatted(path.getFileName(), e.getMessage()));
                        }
                    });
        } catch (IOException e) {
            logger.warning("Failed to scan blocked skin PNGs: %s".formatted(e.getMessage()));
        }
    }

    private boolean isRemoteEntryEnabled(SkinSafetyBlocklistResponse.Entry entry) {
        int minimumSeverity = settings.getProperty(SkinSafetyConfig.PUBLIC_BLOCKLIST_MIN_SEVERITY);
        int severity = entry.getSeverity() == null ? 0 : entry.getSeverity();
        if (severity < minimumSeverity) {
            return false;
        }

        List<String> configuredCategories = settings.getProperty(SkinSafetyConfig.PUBLIC_BLOCKLIST_CATEGORIES);
        if (configuredCategories.isEmpty() || entry.getCategories() == null || entry.getCategories().length == 0) {
            return true;
        }

        Set<String> configured = normalizeStrings(configuredCategories);
        for (String category : entry.getCategories()) {
            if (configured.contains(normalize(category))) {
                return true;
            }
        }
        return false;
    }

    private Map<SkinSafetyDigestAlgorithm, Set<String>> parseDigests(Collection<String> values) {
        Map<SkinSafetyDigestAlgorithm, Set<String>> parsedDigests = new EnumMap<>(SkinSafetyDigestAlgorithm.class);
        for (String value : values) {
            parseDigest(value).ifPresent(digest -> addDigest(parsedDigests, digest));
        }
        return parsedDigests;
    }

    private void addDigest(Map<SkinSafetyDigestAlgorithm, Set<String>> blockedDigests, SkinSafetyDigest digest) {
        blockedDigests.computeIfAbsent(digest.algorithm(), ignored -> new HashSet<>()).add(digest.value());
    }

    private void addNormalizedValue(Set<String> values, @Nullable String value) {
        String normalizedValue = normalize(value);
        if (!normalizedValue.isEmpty()) {
            values.add(normalizedValue);
        }
    }

    private Optional<SkinSafetyDigest> parseDigest(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        Optional<SkinSafetyDigest> digest = SkinSafetyDigest.parse(value);
        if (digest.isEmpty()) {
            logger.warning("Ignoring invalid skin safety digest '%s'".formatted(value));
        }
        return digest;
    }

    private Optional<SkinSafetyDigest> parseRemoteDigest(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        return SkinSafetyDigest.parse(value);
    }

    private Optional<SkinSafetyDigest> parseLegacyDigest(@Nullable String value, SkinSafetyDigestAlgorithm algorithm) {
        String normalizedValue = normalize(value);
        if (normalizedValue.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new SkinSafetyDigest(algorithm, normalizedValue));
    }

    private Set<String> normalizeStrings(Collection<String> values) {
        Set<String> normalized = new HashSet<>();
        for (String value : values) {
            String normalizedValue = normalize(value);
            if (!normalizedValue.isEmpty()) {
                normalized.add(normalizedValue);
            }
        }
        return normalized;
    }

    private Set<UUID> parseUuids(Collection<String> values) {
        Set<UUID> uuids = new HashSet<>();
        for (String value : values) {
            parseUuid(value).ifPresent(uuids::add);
        }
        return uuids;
    }

    private Optional<UUID> parseUuid(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException ignored) {
            logger.warning("Ignoring invalid skin safety UUID '%s'".formatted(value));
            return Optional.empty();
        }
    }

    private String normalize(@Nullable String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
