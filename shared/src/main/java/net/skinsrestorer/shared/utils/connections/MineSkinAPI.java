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
package net.skinsrestorer.shared.utils.connections;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.skinsrestorer.api.SkinVariant;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.IMineSkinAPI;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.exception.TryAgainException;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.Pair;
import net.skinsrestorer.shared.utils.connections.responses.mineskin.MineSkinErrorDelayResponse;
import net.skinsrestorer.shared.utils.connections.responses.mineskin.MineSkinErrorResponse;
import net.skinsrestorer.shared.utils.connections.responses.mineskin.MineSkinUrlResponse;
import net.skinsrestorer.shared.utils.log.SRLogLevel;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class MineSkinAPI implements IMineSkinAPI {
    private final SRLogger logger;
    private final MetricsCounter metricsCounter;
    private final Gson gson = new Gson();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor((Runnable r) -> {
        Thread t = new Thread(r);
        t.setName("SkinsRestorer-MineSkinAPI");
        return t;
    });

    @Override
    public IProperty genSkin(String url, @Nullable SkinVariant skinVariant) throws SkinRequestException {
        AtomicInteger failedAttempts = new AtomicInteger(0);

        do {
            try {
                return genSkinFuture(url, skinVariant).join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof TryAgainException) {
                    failedAttempts.incrementAndGet();
                } else if (e.getCause() instanceof SkinRequestException) {
                    throw new SkinRequestException(e.getCause());
                } else {
                    throw new SkinRequestException(e.getMessage());
                }
            }
        } while (failedAttempts.get() < 5);

        throw new SkinRequestException(Locale.MS_API_FAILED);
    }

    public CompletableFuture<IProperty> genSkinFuture(String url, @Nullable SkinVariant skinVariant) {
        return CompletableFuture.supplyAsync(() -> {
            String skinVariantString = skinVariant != null ? "&variant=" + skinVariant.name().toLowerCase() : "";

            try {
                val response = queryURL("url=" + URLEncoder.encode(url, "UTF-8") + skinVariantString);
                logger.debug("MineSkinAPI: Response: " + response);
                if (!response.isPresent()) // API time out
                    throw new SkinRequestException(Locale.ERROR_UPDATING_SKIN);

                switch (response.get().getLeft()) {
                    case 200:
                        MineSkinUrlResponse urlResponse = gson.fromJson(response.get().getRight(), MineSkinUrlResponse.class);
                        return SkinsRestorerAPI.getApi().createPlatformProperty(IProperty.TEXTURES_NAME,
                                urlResponse.getData().getTexture().getValue(),
                                urlResponse.getData().getTexture().getSignature());
                    case 500:
                    case 400:
                        MineSkinErrorResponse errorResponse = gson.fromJson(response.get().getRight(), MineSkinErrorResponse.class);
                        String error = errorResponse.getError();
                        switch (error) {
                            case "Failed to generate skin data":
                            case "Failed to change skin":
                                logger.debug("[ERROR] MineSkin " + error + ", trying again... ");
                                TimeUnit.SECONDS.sleep(5);

                                throw new TryAgainException(); // try again
                            case "No accounts available":
                                logger.debug("[ERROR] MineSkin " + error + " for: " + url);

                                throw new SkinRequestException(Locale.ERROR_MS_FULL);
                            default:
                                logger.debug("[ERROR] MineSkin Failed! Reason: " + error);
                                throw new SkinRequestException(Locale.ERROR_INVALID_URLSKIN);
                        }
                    case 429:
                        MineSkinErrorDelayResponse errorDelayResponse = gson.fromJson(response.get().getRight(), MineSkinErrorDelayResponse.class);
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
                throw new CompletionException(new SkinRequestException(Locale.ERROR_MS_FULL));
            } catch (JsonSyntaxException e) {
                logger.debug(SRLogLevel.WARNING, "[ERROR] MineSkin Failed! JsonSyntaxException (encoding): (" + url + ") " + e.getLocalizedMessage());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            // throw exception after all tries have failed
            logger.debug("[ERROR] MineSkin Failed! Could not generate skin url: " + url);
            throw new CompletionException(new SkinRequestException(Locale.MS_API_FAILED));
        }, executorService);
    }

    private Optional<Pair<Integer, String>> queryURL(String query) throws IOException {
        for (int i = 0; i < 3; i++) { // try 3 times, if server not responding
            try {
                metricsCounter.increment(MetricsCounter.Service.MINE_SKIN);
                HttpsURLConnection con = (HttpsURLConnection) new URL("https://api.mineskin.org/generate/url/").openConnection();

                con.setRequestMethod("POST");
                con.setRequestProperty("Content-length", String.valueOf(query.length()));
                con.setRequestProperty("Accept", "application/json");
                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                con.setRequestProperty("User-Agent", "SkinsRestorer");
                con.setConnectTimeout(90000);
                con.setReadTimeout(90000);
                con.setDoOutput(true);
                con.setDoInput(true);

                if (!Config.MINESKIN_API_KEY.isEmpty())
                    con.setRequestProperty("Authorization", "Bearer " + Config.MINESKIN_API_KEY);

                DataOutputStream output = new DataOutputStream(con.getOutputStream());
                output.writeBytes(query);
                output.close();
                StringBuilder outStr = new StringBuilder();
                InputStream is;

                try {
                    is = con.getInputStream();
                } catch (Exception e) {
                    is = con.getErrorStream();
                }

                try (DataInputStream input = new DataInputStream(is)) {
                    for (int c = input.read(); c != -1; c = input.read())
                        outStr.append((char) c);
                }

                return Optional.of(new Pair<>(con.getResponseCode(), outStr.toString()));
            } catch (IOException e) {
                if (i == 2)
                    throw e;
            } catch (Exception ignored) {
            }
        }

        return Optional.empty();
    }
}
