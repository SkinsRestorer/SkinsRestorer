/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.skinsrestorer.shared.connections;

import ch.jalu.configme.SettingsManager;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.skinsrestorer.api.SkinVariant;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.MineSkinAPI;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.util.Pair;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.config.MineSkinConfig;
import net.skinsrestorer.shared.connections.http.HttpClient;
import net.skinsrestorer.shared.connections.http.HttpResponse;
import net.skinsrestorer.shared.connections.responses.mineskin.MineSkinErrorDelayResponse;
import net.skinsrestorer.shared.connections.responses.mineskin.MineSkinErrorResponse;
import net.skinsrestorer.shared.connections.responses.mineskin.MineSkinUrlResponse;
import net.skinsrestorer.shared.exception.SkinRequestExceptionShared;
import net.skinsrestorer.shared.exception.TryAgainException;
import net.skinsrestorer.shared.platform.SRPlugin;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.log.SRLogLevel;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MineSkinAPIImpl implements MineSkinAPI {
    private static final String NAMEMC_SKIN_URL = "https://namemc.com/skin/";
    private static final String NAMEMC_IMG_URL = "https://s.namemc.com/i/%s.png";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor((Runnable r) -> {
        Thread t = new Thread(r);
        t.setName("SkinsRestorer-MineSkinAPI");
        return t;
    });
    private final SRLogger logger;
    private final MetricsCounter metricsCounter;
    private final SettingsManager settings;
    private final SkinsRestorerLocale locale;
    private final SRPlugin plugin;

    @Override
    public SkinProperty genSkin(String url, @Nullable SkinVariant skinVariant) throws SkinRequestException {
        url = url.startsWith(NAMEMC_SKIN_URL) ? NAMEMC_IMG_URL.replace("%s", url.substring(24)) : url; // Fix NameMC skins
        AtomicInteger failedAttempts = new AtomicInteger(0);

        do {
            try {
                return genSkinFuture(url, skinVariant).join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof TryAgainException) {
                    failedAttempts.incrementAndGet();
                } else if (e.getCause() instanceof SkinRequestException) {
                    throw new SkinRequestExceptionShared(e.getCause());
                } else {
                    throw new SkinRequestExceptionShared(e.getMessage());
                }
            }
        } while (failedAttempts.get() < 5);

        throw new SkinRequestExceptionShared(locale, Message.ERROR_MS_API_FAILED);
    }

    public CompletableFuture<SkinProperty> genSkinFuture(String url, @Nullable SkinVariant skinVariant) {
        return CompletableFuture.supplyAsync(() -> {
            String skinVariantString = skinVariant != null ? "&variant=" + skinVariant.name().toLowerCase() : "";

            try {
                val response = queryURL("url=" + URLEncoder.encode(url, "UTF-8") + skinVariantString);
                logger.debug("MineSkinAPI: Response: " + response);

                switch (response.getStatusCode()) {
                    case 200:
                        MineSkinUrlResponse urlResponse = response.getBodyAs(MineSkinUrlResponse.class);
                        return SkinProperty.of(urlResponse.getData().getTexture().getValue(),
                                urlResponse.getData().getTexture().getSignature());
                    case 500:
                    case 400:
                        MineSkinErrorResponse errorResponse = response.getBodyAs(MineSkinErrorResponse.class);
                        String error = errorResponse.getError();
                        switch (error) {
                            case "Failed to generate skin data":
                            case "Failed to change skin":
                                logger.debug("[ERROR] MineSkin " + error + ", trying again... ");
                                TimeUnit.SECONDS.sleep(5);

                                throw new TryAgainException(); // try again
                            case "No accounts available":
                                logger.debug("[ERROR] MineSkin " + error + " for: " + url);

                                throw new SkinRequestExceptionShared(locale, Message.ERROR_MS_FULL);
                            default:
                                logger.debug("[ERROR] MineSkin Failed! Reason: " + error);
                                throw new SkinRequestExceptionShared(locale, Message.ERROR_INVALID_URLSKIN);
                        }
                    case 403:
                        MineSkinErrorResponse errorResponse2 = response.getBodyAs(MineSkinErrorResponse.class);
                        String errorCode2 = errorResponse2.getErrorCode();
                        String error2 = errorResponse2.getError();
                        if (errorCode2.equals("invalid_api_key")) {
                            logger.severe("[ERROR] MineSkin API key is not invalid! Reason: " + error2);
                            switch (error2) {
                                case "Invalid API Key":
                                    logger.severe("The API Key provided is not registered on MineSkin! Please empty MineskinAPIKey in plugins/SkinsRestorer/config.yml and run /sr reload");
                                    break;
                                case "Client not allowed":
                                    logger.severe("This server ip is not on the apikey allowed IPs list!");
                                    break;
                                case "Origin not allowed":
                                    logger.severe("This server Origin is not on the apikey allowed Origins list!");
                                    break;
                                case "Agent not allowed":
                                    logger.severe("SkinsRestorer's agent \"SkinsRestorer\" is not on the apikey allowed agents list!");
                                    break;
                            }
                            throw new SkinRequestExceptionShared("Invalid Mineskin API key!, nag the server owner about this!");
                        }
                    case 429:
                        MineSkinErrorDelayResponse errorDelayResponse = response.getBodyAs(MineSkinErrorDelayResponse.class);
                        // If "Too many requests"
                        if (errorDelayResponse.getDelay() != null) {
                            TimeUnit.SECONDS.sleep(errorDelayResponse.getDelay());
                        } else if (errorDelayResponse.getNextRequest() != null) {
                            Instant nextRequestInstant = Instant.ofEpochSecond(errorDelayResponse.getNextRequest());
                            int delay = (int) Duration.between(Instant.now(), nextRequestInstant).getSeconds();

                            if (delay > 0)
                                TimeUnit.SECONDS.sleep(delay);
                        } else { // Should normally not happen
                            TimeUnit.SECONDS.sleep(2);
                        }

                        throw new TryAgainException(); // try again after nextRequest
                }
            } catch (SkinRequestException | TryAgainException e) {
                throw new CompletionException(e);
            } catch (IOException e) {
                logger.debug(SRLogLevel.WARNING, "[ERROR] MineSkin Failed! IOException (connection/disk): (" + url + ") " + e.getLocalizedMessage());
                throw new CompletionException(new SkinRequestExceptionShared(locale, Message.ERROR_MS_FULL));
            } catch (JsonSyntaxException e) {
                logger.debug(SRLogLevel.WARNING, "[ERROR] MineSkin Failed! JsonSyntaxException (encoding): (" + url + ") " + e.getLocalizedMessage());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            // throw exception after all tries have failed
            logger.debug("[ERROR] MineSkin Failed! Could not generate skin url: " + url);
            throw new CompletionException(new SkinRequestExceptionShared(locale, Message.ERROR_MS_API_FAILED));
        }, executorService);
    }

    private HttpResponse queryURL(String query) throws IOException {
        for (int i = 0; true; i++) { // try 3 times, if server not responding
            try {
                metricsCounter.increment(MetricsCounter.Service.MINE_SKIN);

                Map<String, String> headers = new HashMap<>();
                String apiKey = settings.getProperty(MineSkinConfig.MINESKIN_API_KEY);
                if (!apiKey.isEmpty()) {
                    headers.put("Authorization", "Bearer " + apiKey);
                }

                HttpClient client = new HttpClient(
                        "https://api.mineskin.org/generate/url/",
                        new HttpClient.RequestBody(query, HttpClient.HttpType.FORM),
                        HttpClient.HttpType.JSON,
                        plugin.getUserAgent(),
                        HttpClient.HttpMethod.POST,
                        headers,
                        90_000
                );

                return client.execute();
            } catch (IOException e) {
                if (i == 2) {
                    throw e;
                }
            }
        }
    }
}
