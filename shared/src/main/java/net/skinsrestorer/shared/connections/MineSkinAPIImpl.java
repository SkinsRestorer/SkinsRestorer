/*
 * SkinsRestorer
 * Copyright (C) 2024  SkinsRestorer Team
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
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.connections.MineSkinAPI;
import net.skinsrestorer.api.connections.model.MineSkinResponse;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.exception.MineSkinException;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.property.SkinVariant;
import net.skinsrestorer.shared.config.APIConfig;
import net.skinsrestorer.shared.connections.http.HttpClient;
import net.skinsrestorer.shared.connections.http.HttpResponse;
import net.skinsrestorer.shared.connections.mineskin.MineSkinVariant;
import net.skinsrestorer.shared.connections.mineskin.MineSkinVisibility;
import net.skinsrestorer.shared.connections.mineskin.requests.MineSkinUrlRequest;
import net.skinsrestorer.shared.connections.mineskin.responses.MineSkinUrlResponse;
import net.skinsrestorer.shared.exception.DataRequestExceptionShared;
import net.skinsrestorer.shared.exception.MineSkinExceptionShared;
import net.skinsrestorer.shared.log.SRLogLevel;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.SRHelpers;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MineSkinAPIImpl implements MineSkinAPI {
    private static final int MAX_RETRIES = 5;
    private static final String MINESKIN_USER_AGENT = "SkinsRestorer/MineSkinAPI";
    private static final URI MINESKIN_ENDPOINT = URI.create("https://api.mineskin.org/v2/generate");
    private final Semaphore semaphore = new Semaphore(5);
    private final Gson gson = new Gson();
    private final SRLogger logger;
    private final MetricsCounter metricsCounter;
    private final SettingsManager settings;
    private final HttpClient httpClient;
    private final AtomicLong nextRequestAt = new AtomicLong();

    @Override
    public MineSkinResponse genSkin(String imageUrl, @Nullable SkinVariant skinVariant) throws DataRequestException, MineSkinException {
        imageUrl = SRHelpers.sanitizeImageURL(imageUrl);

        try {
            int retryAttempts = 0;
            do {
                semaphore.acquire();
                try {
                    long waitDuration = nextRequestAt.get() - System.currentTimeMillis();
                    if (waitDuration > 0) {
                        logger.debug("[INFO] Waiting %dms before next MineSkin request...".formatted(waitDuration));
                        Thread.sleep(waitDuration);
                    }

                    Optional<MineSkinResponse> optional = genSkinInternal(imageUrl, skinVariant);

                    if (optional.isPresent()) {
                        return optional.get();
                    }
                } catch (IOException e) {
                    logger.debug(SRLogLevel.WARNING, "[ERROR] MineSkin Failed! IOException (connection/disk): (%s)".formatted(imageUrl), e);
                    throw new DataRequestExceptionShared(e);
                } finally {
                    semaphore.release();
                }
            } while (++retryAttempts < MAX_RETRIES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRequestExceptionShared(e);
        }

        throw new MineSkinExceptionShared(Message.ERROR_MS_API_FAILED);
    }

    private Optional<MineSkinResponse> genSkinInternal(String imageUrl, @Nullable SkinVariant skinVariant) throws DataRequestException, MineSkinException, IOException {
        HttpResponse httpResponse = queryURL(imageUrl, skinVariant);
        logger.debug("MineSkinAPI: Response: %s".formatted(httpResponse));

        MineSkinUrlResponse response = httpResponse.getBodyAs(MineSkinUrlResponse.class);

        MineSkinUrlResponse.RateLimit rateLimit = response.getRateLimit();
        if (rateLimit != null) {
            long serverNextRequestAt = System.currentTimeMillis() + rateLimit.getNext().getRelative();
            nextRequestAt.updateAndGet(currentValue -> Math.max(currentValue, serverNextRequestAt));
        }

        if (response.isSuccess()) {
            MineSkinUrlResponse.Skin skin = response.getSkin();
            MineSkinUrlResponse.Skin.Texture.Data textureData = skin.getTexture().getData();
            SkinProperty property = SkinProperty.of(textureData.getValue(), textureData.getSignature());
            return Optional.of(MineSkinResponse.of(property, skin.getUuid(),
                    skinVariant, PropertyUtils.getSkinVariant(property)));
        } else {
            for (MineSkinUrlResponse.Error error : response.getErrors()) {
                logger.debug("[ERROR] MineSkin Failed! Reason: %s Image URL: %s".formatted(error, imageUrl));
                return switch (error.getCode()) {
                    case "rate_limit" -> // try again
                            Optional.empty();
                    case "failed_to_create_id", "skin_change_failed" -> {
                        logger.debug("Trying again in 6 seconds...");
                        long nowPlus = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(6);
                        nextRequestAt.updateAndGet(currentValue -> Math.max(currentValue, nowPlus));

                        yield Optional.empty(); // try again
                    }
                    case "no_account_available" -> throw new MineSkinExceptionShared(Message.ERROR_MS_FULL);
                    case "invalid_api_key" -> {
                        logger.severe("[ERROR] MineSkin API key is invalid! Reason: %s".formatted(error));
                        switch (error.getMessage()) {
                            case "Invalid API Key" ->
                                    logger.severe("The API Key provided is not registered on MineSkin! Please empty \"%s\" in plugins/SkinsRestorer/config.yml and run /sr reload".formatted(APIConfig.MINESKIN_API_KEY.getPath()));
                            case "Client not allowed" ->
                                    logger.severe("This server ip is not on the api key allowed IPs list!");
                            case "Origin not allowed" ->
                                    logger.severe("This server Origin is not on the api key allowed Origins list!");
                            case "Agent not allowed" ->
                                    logger.severe("SkinsRestorer's agent \"%s\" is not on the api key allowed agents list!".formatted(MINESKIN_USER_AGENT));
                            default -> logger.severe("Unknown error, please report this to SkinsRestorer's Discord!");
                        }

                        throw new MineSkinExceptionShared(Message.ERROR_MS_API_KEY_INVALID);
                    }
                    default -> throw new MineSkinExceptionShared(Message.ERROR_INVALID_URLSKIN);
                };
            }

            logger.debug("[ERROR] MineSkin Failed! Unknown error: (Image URL: %s) %d".formatted(imageUrl, httpResponse.statusCode()));
            throw new MineSkinExceptionShared(Message.ERROR_MS_API_FAILED);
        }
    }

    private HttpResponse queryURL(String url, @Nullable SkinVariant skinVariant) throws IOException {
        for (int i = 0; true; i++) { // try 3 times if server not responding
            try {
                metricsCounter.increment(MetricsCounter.Service.MINESKIN_CALLS);

                Map<String, String> headers = new HashMap<>();
                getApiKey(settings).ifPresent(s ->
                        headers.put("Authorization", "Bearer %s".formatted(s)));

                return httpClient.execute(
                        MINESKIN_ENDPOINT,
                        new HttpClient.RequestBody(gson.toJson(new MineSkinUrlRequest(
                                skinVariant == null ? MineSkinVariant.UNKNOWN : switch (skinVariant) {
                                    case CLASSIC -> MineSkinVariant.CLASSIC;
                                    case SLIM -> MineSkinVariant.SLIM;
                                },
                                null,
                                settings.getProperty(APIConfig.MINESKIN_SECRET_SKINS)
                                        ? MineSkinVisibility.UNLISTED : MineSkinVisibility.PUBLIC,
                                null,
                                url
                        )), HttpClient.HttpType.JSON),
                        HttpClient.HttpType.JSON,
                        MINESKIN_USER_AGENT,
                        HttpClient.HttpMethod.POST,
                        headers,
                        90_000
                );
            } catch (IOException e) {
                if (i >= 2) {
                    throw new IOException(e);
                }
            }
        }
    }

    private Optional<String> getApiKey(SettingsManager settings) {
        String apiKey = settings.getProperty(APIConfig.MINESKIN_API_KEY);
        if (apiKey.isEmpty() || apiKey.equals("key")) {
            return Optional.empty();
        }

        return Optional.of(apiKey);
    }
}
