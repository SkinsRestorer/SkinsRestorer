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
import net.skinsrestorer.shared.connections.requests.mineskin.MineSkinUrlRequest;
import net.skinsrestorer.shared.connections.responses.mineskin.MineSkinErrorDelayResponse;
import net.skinsrestorer.shared.connections.responses.mineskin.MineSkinErrorResponse;
import net.skinsrestorer.shared.connections.responses.mineskin.MineSkinTexture;
import net.skinsrestorer.shared.connections.responses.mineskin.MineSkinUrlResponse;
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
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MineSkinAPIImpl implements MineSkinAPI {
    private static final int MAX_RETRIES = 5;
    private static final String MINESKIN_USER_AGENT = "SkinsRestorer/MineSkinAPI";
    private static final URI MINESKIN_ENDPOINT = URI.create("https://api.mineskin.org/generate/url");
    private final ReentrantLock lock = new ReentrantLock();
    private final Gson gson = new Gson();
    private final SRLogger logger;
    private final MetricsCounter metricsCounter;
    private final SettingsManager settings;
    private final HttpClient httpClient;

    @Override
    public MineSkinResponse genSkin(String imageUrl, @Nullable SkinVariant skinVariant) throws DataRequestException, MineSkinException {
        imageUrl = SRHelpers.sanitizeImageURL(imageUrl);

        int retryAttempts = 0;
        do {
            lock.lock();
            try {
                Optional<MineSkinResponse> optional = genSkinInternal(imageUrl, skinVariant);

                if (optional.isPresent()) {
                    return optional.get();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                logger.debug(SRLogLevel.WARNING, "[ERROR] MineSkin Failed! IOException (connection/disk): (%s)".formatted(imageUrl), e);
                throw new DataRequestExceptionShared(e);
            } finally {
                lock.unlock();
            }
        } while (++retryAttempts < MAX_RETRIES);

        throw new MineSkinExceptionShared(Message.ERROR_MS_API_FAILED);
    }

    private Optional<MineSkinResponse> genSkinInternal(String imageUrl, @Nullable SkinVariant skinVariant) throws DataRequestException, MineSkinException, IOException, InterruptedException {
        HttpResponse httpResponse = queryURL(imageUrl, skinVariant);
        logger.debug("MineSkinAPI: Response: %s".formatted(httpResponse));

        switch (httpResponse.statusCode()) {
            case 200 -> {
                MineSkinUrlResponse response = httpResponse.getBodyAs(MineSkinUrlResponse.class);
                MineSkinTexture texture = response.getData().getTexture();
                SkinProperty property = SkinProperty.of(texture.getValue(), texture.getSignature());
                return Optional.of(MineSkinResponse.of(property, response.getIdStr(),
                        skinVariant, PropertyUtils.getSkinVariant(property)));
            }
            case 500, 400 -> {
                MineSkinErrorResponse response = httpResponse.getBodyAs(MineSkinErrorResponse.class);
                String error = response.getErrorCode();
                logger.debug("[ERROR] MineSkin Failed! Reason: %s Image URL: %s".formatted(error, imageUrl));
                // try again
                return switch (error) {
                    case "failed_to_create_id", "skin_change_failed" -> {
                        logger.debug("Trying again in 6 seconds...");
                        TimeUnit.SECONDS.sleep(6);
                        yield Optional.empty();
                    }
                    case "no_account_available" -> throw new MineSkinExceptionShared(Message.ERROR_MS_FULL);
                    default -> throw new MineSkinExceptionShared(Message.ERROR_INVALID_URLSKIN);
                };
            }
            case 403 -> {
                MineSkinErrorResponse response = httpResponse.getBodyAs(MineSkinErrorResponse.class);
                String errorCode = response.getErrorCode();
                String error = response.getError();
                if (errorCode.equals("invalid_api_key")) {
                    logger.severe("[ERROR] MineSkin API key is invalid! Reason: %s".formatted(error));
                    switch (error) {
                        case "Invalid API Key" ->
                                logger.severe("The API Key provided is not registered on MineSkin! Please empty \"%s\" in plugins/SkinsRestorer/config.yml and run /sr reload".formatted(APIConfig.MINESKIN_API_KEY.getPath()));
                        case "Client not allowed" ->
                                logger.severe("This server ip is not on the api key allowed IPs list!");
                        case "Origin not allowed" ->
                                logger.severe("This server Origin is not on the api key allowed Origins list!");
                        case "Agent not allowed" ->
                                logger.severe("SkinsRestorer's agent \"%s\" is not on the api key allowed agents list!".formatted(MINESKIN_USER_AGENT));
                        default -> logger.severe("Unknown error, please report this to SkinsRestorer's discord!");
                    }

                    throw new MineSkinExceptionShared(Message.ERROR_MS_API_KEY_INVALID);
                }

                throw new MineSkinExceptionShared(Message.ERROR_MS_UNKNOWN);
            }
            case 429 -> {
                MineSkinErrorDelayResponse response = httpResponse.getBodyAs(MineSkinErrorDelayResponse.class);

                // If "Too many requests"
                if (response.getDelay() != null) {
                    TimeUnit.SECONDS.sleep(response.getDelay());
                } else if (response.getNextRequest() != null) {
                    Instant nextRequestInstant = Instant.ofEpochSecond(response.getNextRequest());
                    int delay = (int) Duration.between(Instant.now(), nextRequestInstant).getSeconds();

                    if (delay > 0) {
                        TimeUnit.SECONDS.sleep(delay);
                    }
                } else { // Should normally not happen
                    TimeUnit.SECONDS.sleep(6);
                }

                return Optional.empty(); // try again after nextRequest
            }
            default -> {
                logger.debug("[ERROR] MineSkin Failed! Unknown error: (Image URL: %s) %d".formatted(imageUrl, httpResponse.statusCode()));
                throw new MineSkinExceptionShared(Message.ERROR_MS_API_FAILED);
            }
        }
    }

    private HttpResponse queryURL(String url, @Nullable SkinVariant skinVariant) throws IOException {
        for (int i = 0; true; i++) { // try 3 times if server not responding
            try {
                metricsCounter.increment(MetricsCounter.Service.MINE_SKIN);

                Map<String, String> headers = new HashMap<>();
                Optional<String> apiKey = getApiKey(settings);
                apiKey.ifPresent(s -> headers.put("Authorization", "Bearer %s".formatted(s)));

                return httpClient.execute(
                        MINESKIN_ENDPOINT,
                        new HttpClient.RequestBody(gson.toJson(new MineSkinUrlRequest(
                                skinVariant,
                                null,
                                settings.getProperty(APIConfig.MINESKIN_SECRET_SKINS) ? 1 : 0,
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
