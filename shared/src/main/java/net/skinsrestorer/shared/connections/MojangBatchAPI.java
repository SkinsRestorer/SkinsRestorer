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
package net.skinsrestorer.shared.connections;

import ch.jalu.configme.SettingsManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.shared.config.APIConfig;
import net.skinsrestorer.shared.connections.http.HttpClient;
import net.skinsrestorer.shared.connections.http.HttpResponse;
import net.skinsrestorer.shared.connections.responses.uuid.MojangBatchUUIDEntry;
import net.skinsrestorer.shared.exception.DataRequestExceptionShared;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.RateLimitBackoff;
import net.skinsrestorer.shared.utils.UUIDUtils;
import net.skinsrestorer.shared.utils.ValidationUtil;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class MojangBatchAPI {
    private static final int DEFAULT_BATCH_SIZE = 10;
    private static final int HTTP_TIMEOUT_MS = 5000;

    private final MetricsCounter metricsCounter;
    private final SRLogger logger;
    private final SRPlatformAdapter adapter;
    private final HttpClient httpClient;
    private final SettingsManager settings;
    private final String batchEndpoint;
    private final String userAgent;
    private final RateLimitBackoff rateLimitBackoff;
    private final ConcurrentHashMap<String, CompletableFuture<Optional<UUID>>> pendingRequests = new ConcurrentHashMap<>();
    private final List<String> batchQueue = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong lastBatchTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicBoolean batchInProgress = new AtomicBoolean(false);

    public MojangBatchAPI(MetricsCounter metricsCounter, SRLogger logger, SRPlatformAdapter adapter, HttpClient httpClient, SettingsManager settings, String batchEndpoint, String userAgent, RateLimitBackoff rateLimitBackoff) {
        this.metricsCounter = metricsCounter;
        this.logger = logger;
        this.adapter = adapter;
        this.httpClient = httpClient;
        this.settings = settings;
        this.batchEndpoint = batchEndpoint;
        this.userAgent = userAgent;
        this.rateLimitBackoff = rateLimitBackoff;
    }

    public CompletableFuture<Optional<UUID>> getUUID(String playerName) {
        if (ValidationUtil.invalidMinecraftUsername(playerName)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        // Check if already pending
        CompletableFuture<Optional<UUID>> existing = pendingRequests.get(playerName.toLowerCase(Locale.ROOT));
        if (existing != null) {
            return existing;
        }

        CompletableFuture<Optional<UUID>> future = new CompletableFuture<>();
        pendingRequests.put(playerName.toLowerCase(Locale.ROOT), future);

        synchronized (batchQueue) {
            batchQueue.add(playerName);
            if (batchQueue.size() >= DEFAULT_BATCH_SIZE) {
                // Trigger batch immediately if we have 10 requests
                scheduleBatch(0);
            } else {
                // Schedule batch after window time
                long timeSinceLastBatch = System.currentTimeMillis() - lastBatchTime.get();
                int windowSeconds = settings.getProperty(APIConfig.MOJANG_BATCH_WINDOW_SECONDS);
                long delay = Math.max(0, TimeUnit.SECONDS.toMillis(windowSeconds) - timeSinceLastBatch);
                scheduleBatch(delay);
            }
        }

        return future;
    }

    private void scheduleBatch(long delayMillis) {
        adapter.runAsyncDelayed(this::processBatch, delayMillis, TimeUnit.MILLISECONDS);
    }

    private void processBatch() {
        if (!batchInProgress.compareAndSet(false, true)) {
            return; // Another batch is in progress
        }

        List<String> namesToProcess;
        synchronized (batchQueue) {
            if (batchQueue.isEmpty()) {
                batchInProgress.set(false);
                return;
            }
            namesToProcess = new ArrayList<>(batchQueue);
            batchQueue.clear();
        }

        lastBatchTime.set(System.currentTimeMillis());

        try {
            List<Optional<UUID>> results = sendBatchRequest(namesToProcess);
            for (int i = 0; i < namesToProcess.size(); i++) {
                String name = namesToProcess.get(i);
                Optional<UUID> result = i < results.size() ? results.get(i) : Optional.empty();
                CompletableFuture<Optional<UUID>> future = pendingRequests.remove(name.toLowerCase(Locale.ROOT));
                if (future != null) {
                    future.complete(result);
                }
            }
        } catch (Exception e) {
            logger.debug("Error processing batch request", e);
            // Propagate failures so callers can try another provider.
            for (String name : namesToProcess) {
                CompletableFuture<Optional<UUID>> future = pendingRequests.remove(name.toLowerCase(Locale.ROOT));
                if (future != null) {
                    future.completeExceptionally(e);
                }
            }
        } finally {
            batchInProgress.set(false);
        }
    }

    private List<Optional<UUID>> sendBatchRequest(List<String> names) throws DataRequestExceptionShared {
        if (names.isEmpty()) {
            return Collections.emptyList();
        }

        if (rateLimitBackoff.isRateLimited(batchEndpoint)) {
            long remainingMs = rateLimitBackoff.getRemainingMs(batchEndpoint);
            throw new DataRequestExceptionShared("Rate limited, retrying in %d seconds. (Rate Limited)".formatted(
                    TimeUnit.MILLISECONDS.toSeconds(remainingMs)));
        }

        // Limit to 10 names as per API
        List<String> batchNames = names.size() > DEFAULT_BATCH_SIZE ? names.subList(0, DEFAULT_BATCH_SIZE) : names;

        Gson gson = new Gson();
        HttpClient.RequestBody requestBody = new HttpClient.RequestBody(gson.toJson(batchNames), HttpClient.HttpType.JSON);

        try {
            HttpResponse httpResponse = httpClient.execute(
                    URI.create(batchEndpoint),
                    requestBody,
                    HttpClient.HttpType.JSON,
                    userAgent,
                    HttpClient.HttpMethod.POST,
                    Collections.emptyMap(),
                    HTTP_TIMEOUT_MS
            );

            metricsCounter.increment(MetricsCounter.Service.MOJANG_UUID);

            if (httpResponse.statusCode() == 200 && !httpResponse.body().isEmpty()) {
                List<MojangBatchUUIDEntry> entries = httpResponse.getBodyAs(
                        new TypeToken<List<MojangBatchUUIDEntry>>() {
                        }.getType()
                );
                return processBatchResponse(entries, batchNames);
            } else if (httpResponse.statusCode() == 429) {
                rateLimitBackoff.markRateLimited(batchEndpoint, httpResponse);
                throw new DataRequestExceptionShared("Please wait a minute before requesting that skin again. (Rate Limited)");
            } else {
                throw new DataRequestExceptionShared("Mojang batch request failed with status: %d".formatted(httpResponse.statusCode()));
            }
        } catch (IOException e) {
            logger.debug("Error sending batch request", e);
            throw new DataRequestExceptionShared(e);
        } catch (DataRequestException e) {
            logger.debug("Error parsing batch response", e);
            throw new DataRequestExceptionShared(e);
        }
    }

    private List<Optional<UUID>> processBatchResponse(List<MojangBatchUUIDEntry> entries, List<String> requestedNames) {
        List<Optional<UUID>> results = new ArrayList<>();
        Map<String, UUID> nameToUuid = new HashMap<>();

        if (entries != null) {
            for (MojangBatchUUIDEntry entry : entries) {
                if (entry.getId() != null && entry.getName() != null) {
                    try {
                        UUID uuid = UUIDUtils.convertToDashed(entry.getId());
                        nameToUuid.put(entry.getName().toLowerCase(Locale.ROOT), uuid);
                    } catch (Exception e) {
                        logger.debug("Invalid UUID format: " + entry.getId(), e);
                    }
                }
            }
        }

        for (String name : requestedNames) {
            UUID uuid = nameToUuid.get(name.toLowerCase(Locale.ROOT));
            results.add(Optional.ofNullable(uuid));
        }

        return results;
    }
}
