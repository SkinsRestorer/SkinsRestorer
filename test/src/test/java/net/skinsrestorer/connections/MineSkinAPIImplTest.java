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
package net.skinsrestorer.connections;

import ch.jalu.configme.SettingsManager;
import com.google.gson.Gson;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.connections.model.MineSkinResponse;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.exception.MineSkinException;
import net.skinsrestorer.api.property.SkinVariant;
import net.skinsrestorer.shared.config.APIConfig;
import net.skinsrestorer.shared.connections.MineSkinAPIImpl;
import net.skinsrestorer.shared.connections.http.HttpClient;
import net.skinsrestorer.shared.connections.http.HttpResponse;
import net.skinsrestorer.shared.exception.DataRequestExceptionShared;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.utils.MetricsCounter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MineSkinAPIImplTest {
    private static final String AXOLOTL_URL = "skinsrestorer-axolotl://v2/"
            + "g-AzMkTUndogEcMsh9O0OVqOhDiSRWdtvHEj9RXdMEc5vti6rYXTeWGimbxTDwVnNrLLK_Jes7k6weszTHvDow";
    private static final String SKIN_ID = "a759ae8e2c4e4f45a3122da8c6832bfc";
    private static final URI LOOKUP_URI = URI.create("https://api.mineskin.org/v2/skins/" + SKIN_ID);
    private static final Gson GSON = new Gson();
    private final HttpClient httpClient = mock(HttpClient.class);
    private final SettingsManager settings = mock(SettingsManager.class);
    private final MetricsCounter metricsCounter = mock(MetricsCounter.class);
    private final MineSkinAPIImpl api = new MineSkinAPIImpl(mock(SRLogger.class), metricsCounter,
            settings, httpClient);

    @ParameterizedTest
    @ValueSource(ints = {201, 204, 301, 404, 502, 503})
    void unexpectedStatusesRejectEvenValidApiErrors(int status) throws Exception {
        respond(new HttpResponse(status, "{\"errors\":[{\"code\":\"invalid_api_key\"}]}", Map.of()));

        assertThrows(DataRequestException.class, () -> api.genSkin("https://example.com/skin.png", null));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 500})
    void expectedErrorStatusesUseApiErrorHandling(int status) throws Exception {
        respond(new HttpResponse(status,
                "{\"errors\":[{\"code\":\"invalid_api_key\",\"message\":\"Invalid API Key\"}]}", Map.of()));

        assertThrows(MineSkinException.class, () -> api.genSkin("https://example.com/skin.png", null));
    }

    @Test
    void rateLimitsStillRetry() throws Exception {
        respond(new HttpResponse(429, "{\"errors\":[{\"code\":\"rate_limit\"}]}", Map.of()),
                new HttpResponse(400, "{\"errors\":[{\"code\":\"invalid_image\"}]}", Map.of()));

        assertThrows(MineSkinException.class, () -> api.genSkin("https://example.com/skin.png", null));
        verify(httpClient, times(2)).execute(any(), any(), any(), any(), any(), any(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"a759ae8e-2c4e-4f45-a312-2da8c6832bfc", SKIN_ID})
    void axolotlV2LooksUpExistingSignedSkin(String id) throws Exception {
        String value = textureValue(SkinVariant.SLIM);
        respond(decrypted("https://minesk.in/" + id), signedSkin(value));
        when(settings.getProperty(APIConfig.MINESKIN_API_KEY)).thenReturn("test-key");

        MineSkinResponse result = api.genSkin(AXOLOTL_URL, SkinVariant.CLASSIC);

        assertEquals(value, result.getProperty().getValue());
        assertEquals("original-signature", result.getProperty().getSignature());
        assertEquals("https://textures.minecraft.net/texture/cape",
                PropertyUtils.getSkinProfileData(result.getProperty()).getTextures().getCAPE().getUrl());
        assertEquals(SkinVariant.SLIM, result.getGeneratedVariant());
        assertEquals(SkinVariant.CLASSIC, result.getRequestedVariant());
        assertEquals(SKIN_ID, result.getMineSkinId());
        verify(httpClient).execute(eq(URI.create("https://axolotl.skinsrestorer.net/mineskin/decrypt-url?encryptedUrl="
                        + URLEncoder.encode(AXOLOTL_URL, StandardCharsets.UTF_8))), isNull(), eq(HttpClient.HttpType.JSON),
                eq("SkinsRestorer/MineSkinAPI"), eq(HttpClient.HttpMethod.GET), eq(Map.of()), eq(30_000));
        verify(httpClient).execute(eq(LOOKUP_URI), isNull(), eq(HttpClient.HttpType.JSON),
                eq("SkinsRestorer/MineSkinAPI"), eq(HttpClient.HttpMethod.GET),
                eq(Map.of("Authorization", "Bearer test-key")), eq(30_000));
        verify(httpClient, times(2)).execute(any(), any(), any(), any(), any(), any(), anyInt());
        verify(httpClient, never()).execute(any(), any(), any(), any(), eq(HttpClient.HttpMethod.POST), any(), anyInt());
    }

    @Test
    void axolotlPreservesClassicVariantWithoutHint() throws Exception {
        String value = textureValue(SkinVariant.CLASSIC);
        respond(decrypted("https://minesk.in/" + SKIN_ID), signedSkin(value));

        MineSkinResponse result = api.genSkin(AXOLOTL_URL, null);

        assertEquals(value, result.getProperty().getValue());
        assertEquals(SkinVariant.CLASSIC, result.getGeneratedVariant());
        assertNull(result.getRequestedVariant());
    }

    @Test
    void deployedV2EnvelopeAlsoPreservesTheSignedTexture() throws Exception {
        String value = textureValue(SkinVariant.SLIM);
        respond(decrypted("https://minesk.in/" + SKIN_ID), generatedSkin(value));

        MineSkinResponse result = api.genSkin(AXOLOTL_URL, SkinVariant.CLASSIC);

        assertEquals(value, result.getProperty().getValue());
        assertEquals("original-signature", result.getProperty().getSignature());
        assertEquals(SkinVariant.SLIM, result.getGeneratedVariant());
        assertEquals(SkinVariant.CLASSIC, result.getRequestedVariant());
        assertEquals(SKIN_ID, result.getMineSkinId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "null"})
    void unsuccessfulEnvelopesNeverReturnTheirSkin(String success) throws Exception {
        String body = "{\"success\":" + success + ",\"skin\":"
                + signedSkin(textureValue(SkinVariant.CLASSIC)).body() + "}";
        respond(decrypted("https://minesk.in/" + SKIN_ID), new HttpResponse(200, body, Map.of()));

        assertThrows(DataRequestException.class, () -> api.genSkin(AXOLOTL_URL, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not a URL", "", "https://example.com/skin.png", "https://minesk.in/",
            "https://minesk.in/not-a-uuid", "https://minesk.in/1-2-3-4-5",
            "https://minesk.in/../../generate", "https://minesk.in/" + SKIN_ID + "/extra",
            "https://minesk.in.evil.example/" + SKIN_ID, "https://minesk.in@evil.example/" + SKIN_ID,
            "http://minesk.in/" + SKIN_ID})
    void malformedDecryptedUrlsNeverGenerateOrLookup(String url) throws Exception {
        respond(decrypted(url));

        assertThrows(DataRequestException.class, () -> api.genSkin(AXOLOTL_URL, null));
        verify(httpClient, times(1)).execute(any(), any(), any(), any(), any(), any(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "{\"url\":null}", "not-json"})
    void invalidDecryptResponsesAreRequestFailures(String body) throws Exception {
        respond(new HttpResponse(200, body, Map.of()));

        assertThrows(DataRequestException.class, () -> api.genSkin(AXOLOTL_URL, null));
        verify(httpClient, times(1)).execute(any(), any(), any(), any(), any(), any(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 404, 429, 500, 503})
    void decryptApiFailuresNeverReachMineSkin(int status) throws Exception {
        respond(new HttpResponse(status, "{\"error\":\"unavailable\"}", Map.of()));

        assertThrows(DataRequestException.class, () -> api.genSkin(AXOLOTL_URL, null));
        verify(httpClient, times(1)).execute(any(), any(), any(), any(), any(), any(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 404, 429, 500, 503})
    void lookupApiFailuresNeverFallBackToGeneration(int status) throws Exception {
        respond(decrypted("https://minesk.in/" + SKIN_ID), new HttpResponse(status, "{}", Map.of()));

        assertThrows(DataRequestException.class, () -> api.genSkin(AXOLOTL_URL, null));
        verify(httpClient, times(2)).execute(any(), any(), any(), any(), any(), any(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "not-json", "{\"success\":false}",
            "{\"success\":true,\"skin\":{}}",
            "{\"success\":true,\"skin\":{\"texture\":{\"data\":{}}}}",
            "{\"success\":true,\"skin\":{\"uuid\":\"id\",\"texture\":{\"data\":{\"value\":\"unsigned\"}}}}",
            "[]", "{\"uuid\":\"id\"}", "{\"uuid\":\"id\",\"texture\":{}}",
            "{\"uuid\":\"id\",\"texture\":{\"data\":{\"value\":\"unsigned\"}}}"})
    void invalidLookupResponsesAreRequestFailures(String body) throws Exception {
        respond(decrypted("https://minesk.in/" + SKIN_ID), new HttpResponse(200, body, Map.of()));

        assertThrows(DataRequestException.class, () -> api.genSkin(AXOLOTL_URL, null));
    }

    @Test
    void invalidLookupTextureBase64IsAHandledRequestFailure() throws Exception {
        respond(decrypted("https://minesk.in/" + SKIN_ID), signedSkin("%%%not-base64%%%"));

        DataRequestExceptionShared error = assertThrowsExactly(DataRequestExceptionShared.class,
                () -> api.genSkin(AXOLOTL_URL, null));

        assertInstanceOf(IllegalArgumentException.class, error.getCause());
        verify(httpClient, times(2)).execute(any(), any(), any(), any(), any(), any(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-json", "null", "{}", "{\"textures\":null}", "{\"textures\":{}}",
            "{\"textures\":{\"SKIN\":null}}", "{\"textures\":{\"SKIN\":[]}}"})
    void malformedLookupTextureProfilesAreHandledRequestFailures(String texture) throws Exception {
        String value = Base64.getEncoder().encodeToString(texture.getBytes(StandardCharsets.UTF_8));
        respond(decrypted("https://minesk.in/" + SKIN_ID), signedSkin(value));

        DataRequestExceptionShared error = assertThrowsExactly(DataRequestExceptionShared.class,
                () -> api.genSkin(AXOLOTL_URL, null));

        assertInstanceOf(RuntimeException.class, error.getCause());
        verify(httpClient, times(2)).execute(any(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void decryptConnectionFailureNeverReachesMineSkin() throws Exception {
        when(httpClient.execute(any(), any(), any(), any(), any(), any(), anyInt()))
                .thenThrow(new IOException("Connection failed"));

        assertThrows(DataRequestException.class, () -> api.genSkin(AXOLOTL_URL, null));
        verify(httpClient, times(1)).execute(any(), any(), any(), any(), any(), any(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void lookupRetriesTransientConnectionFailures(int failures) throws Exception {
        String value = textureValue(SkinVariant.SLIM);
        respond(decrypted("https://minesk.in/" + SKIN_ID));
        AtomicInteger attempts = new AtomicInteger();
        when(httpClient.execute(eq(LOOKUP_URI), any(), any(), any(), any(), any(), anyInt()))
                .thenAnswer(invocation -> {
                    if (attempts.getAndIncrement() < failures) {
                        throw new IOException("Temporary connection failure");
                    }
                    return signedSkin(value);
                });

        MineSkinResponse result = api.genSkin(AXOLOTL_URL, SkinVariant.CLASSIC);

        assertEquals(value, result.getProperty().getValue());
        assertEquals("original-signature", result.getProperty().getSignature());
        assertEquals(SkinVariant.SLIM, result.getGeneratedVariant());
        assertEquals(SkinVariant.CLASSIC, result.getRequestedVariant());
        verify(httpClient, times(failures + 1)).execute(eq(LOOKUP_URI), isNull(), eq(HttpClient.HttpType.JSON),
                eq("SkinsRestorer/MineSkinAPI"), eq(HttpClient.HttpMethod.GET), eq(Map.of()), eq(30_000));
        verify(metricsCounter, times(failures + 1)).increment(MetricsCounter.Service.MINESKIN_CALLS);
        verify(httpClient, times(failures + 2)).execute(any(), any(), any(), any(), any(), any(), anyInt());
        verify(httpClient, never()).execute(any(), any(), any(), any(), eq(HttpClient.HttpMethod.POST), any(), anyInt());
    }

    @Test
    void lookupConnectionFailureNeverFallsBackToGeneration() throws Exception {
        respond(decrypted("https://minesk.in/" + SKIN_ID));
        IOException failure = new IOException("Connection failed");
        when(httpClient.execute(eq(LOOKUP_URI), any(), any(), any(), any(), any(), anyInt()))
                .thenThrow(failure);

        DataRequestExceptionShared error = assertThrowsExactly(DataRequestExceptionShared.class,
                () -> api.genSkin(AXOLOTL_URL, null));

        assertSame(failure, error.getCause());
        verify(httpClient, times(3)).execute(eq(LOOKUP_URI), any(), any(), any(), any(), any(), anyInt());
        verify(metricsCounter, times(3)).increment(MetricsCounter.Service.MINESKIN_CALLS);
        verify(httpClient, times(4)).execute(any(), any(), any(), any(), any(), any(), anyInt());
        verify(httpClient, never()).execute(any(), any(), any(), any(), eq(HttpClient.HttpMethod.POST), any(), anyInt());
    }

    @Test
    @Timeout(10)
    void lookupHonorsRetryAfterBeforeRetrying() throws Exception {
        respond(decrypted("https://minesk.in/" + SKIN_ID));
        AtomicLong firstRequest = new AtomicLong();
        AtomicLong secondRequest = new AtomicLong();
        when(httpClient.execute(eq(LOOKUP_URI), any(), any(), any(), any(), any(), anyInt()))
                .thenAnswer(invocation -> {
                    firstRequest.set(System.nanoTime());
                    return new HttpResponse(429, "{}", Map.of("Retry-After", List.of("1")));
                }).thenAnswer(invocation -> {
                    secondRequest.set(System.nanoTime());
                    return signedSkin(textureValue(SkinVariant.SLIM));
                });

        MineSkinResponse result = api.genSkin(AXOLOTL_URL, null);

        assertEquals(SkinVariant.SLIM, result.getGeneratedVariant());
        assertTrue(secondRequest.get() - firstRequest.get() >= TimeUnit.MILLISECONDS.toNanos(900));
        verify(httpClient, times(2)).execute(eq(LOOKUP_URI), any(), any(), any(), any(), any(), anyInt());
        verify(metricsCounter, times(2)).increment(MetricsCounter.Service.MINESKIN_CALLS);
    }

    @Test
    @Timeout(10)
    void repeatedRateLimitsHaveABoundedRetryCount() throws Exception {
        respond(decrypted("https://minesk.in/" + SKIN_ID));
        when(httpClient.execute(eq(LOOKUP_URI), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(new HttpResponse(429, "{}", Map.of("Retry-After", List.of("1"))));

        assertThrows(DataRequestException.class, () -> api.genSkin(AXOLOTL_URL, null));
        verify(httpClient, times(5)).execute(eq(LOOKUP_URI), any(), any(), any(), any(), any(), anyInt());
        verify(metricsCounter, times(5)).increment(MetricsCounter.Service.MINESKIN_CALLS);
        verify(httpClient, never()).execute(any(), any(), any(), any(), eq(HttpClient.HttpMethod.POST), any(), anyInt());
    }

    @Test
    @Timeout(5)
    void excessiveBackoffFailsWithoutSendingAnotherLookupOrDelayingGeneration() throws Exception {
        respond(decrypted("https://minesk.in/" + SKIN_ID));
        when(httpClient.execute(eq(LOOKUP_URI), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(new HttpResponse(429, "{}", Map.of("Retry-After", List.of("60"))));
        when(httpClient.execute(eq(URI.create("https://api.mineskin.org/v2/generate")),
                any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(generatedSkin(textureValue(SkinVariant.CLASSIC)));

        assertThrows(DataRequestException.class, () -> api.genSkin(AXOLOTL_URL, null));
        assertThrows(DataRequestException.class, () -> api.genSkin(AXOLOTL_URL, null));
        assertEquals(SkinVariant.CLASSIC, api.genSkin("https://example.com/skin.png", null).getGeneratedVariant());
        verify(httpClient, times(1)).execute(eq(LOOKUP_URI), any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void interruptedLookupPreservesTheInterruptFlag() throws Exception {
        respond(decrypted("https://minesk.in/" + SKIN_ID));
        try {
            Thread.currentThread().interrupt();
            DataRequestException error = assertThrows(DataRequestException.class, () -> api.genSkin(AXOLOTL_URL, null));
            assertInstanceOf(InterruptedException.class, error.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
            verify(httpClient, never()).execute(eq(LOOKUP_URI), any(), any(), any(), any(), any(), anyInt());
        } finally {
            Thread.interrupted();
        }
    }

    /** Holds five lookup requests open and verifies that the sixth cannot enter until a permit is released. */
    @Test
    @Timeout(10)
    void concurrentLookupsNeverExceedFiveRequests() throws Exception {
        respond(decrypted("https://minesk.in/" + SKIN_ID));
        CountDownLatch firstFive = new CountDownLatch(5);
        CountDownLatch sixth = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger requests = new AtomicInteger();
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximumActive = new AtomicInteger();
        when(httpClient.execute(eq(LOOKUP_URI), any(), any(), any(), any(), any(), anyInt()))
                .thenAnswer(invocation -> {
                    maximumActive.accumulateAndGet(active.incrementAndGet(), Math::max);
                    if (requests.incrementAndGet() == 6) {
                        sixth.countDown();
                    }
                    firstFive.countDown();
                    try {
                        assertTrue(release.await(5, TimeUnit.SECONDS));
                        return signedSkin(textureValue(SkinVariant.CLASSIC));
                    } finally {
                        active.decrementAndGet();
                    }
                });
        ExecutorService executor = Executors.newFixedThreadPool(6);
        try {
            List<Future<MineSkinResponse>> results = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                results.add(executor.submit(() -> api.genSkin(AXOLOTL_URL, null)));
            }
            assertTrue(firstFive.await(5, TimeUnit.SECONDS));
            assertFalse(sixth.await(200, TimeUnit.MILLISECONDS));
            release.countDown();
            for (Future<MineSkinResponse> result : results) {
                assertEquals(SkinVariant.CLASSIC, result.get(5, TimeUnit.SECONDS).getGeneratedVariant());
            }
            assertEquals(5, maximumActive.get());
            assertEquals(6, requests.get());
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void normalImageUrlsStillUseGeneration() throws Exception {
        String value = textureValue(SkinVariant.SLIM);
        respond(generatedSkin(value));

        MineSkinResponse result = api.genSkin("https://example.com/skin.png", SkinVariant.SLIM);

        assertEquals(value, result.getProperty().getValue());
        assertEquals("original-signature", result.getProperty().getSignature());
        assertEquals(SkinVariant.SLIM, result.getGeneratedVariant());
        verify(httpClient).execute(eq(URI.create("https://api.mineskin.org/v2/generate")),
                argThat(body -> body.body().contains("https://example.com/skin.png")
                        && body.body().contains("slim")),
                eq(HttpClient.HttpType.JSON), eq("SkinsRestorer/MineSkinAPI"),
                eq(HttpClient.HttpMethod.POST), eq(Map.of()), eq(90_000));
        verifyNoMoreInteractions(httpClient);
    }

    private HttpResponse decrypted(String url) {
        return new HttpResponse(200, GSON.toJson(Map.of("url", url)), Map.of());
    }

    private HttpResponse signedSkin(String value) {
        return new HttpResponse(200, """
                {"uuid":"%s","texture":{"data":{
                "value":"%s","signature":"original-signature"}}}
                """.formatted(SKIN_ID, value), Map.of());
    }

    private HttpResponse generatedSkin(String value) {
        return new HttpResponse(200, "{\"success\":true,\"skin\":" + signedSkin(value).body() + "}", Map.of());
    }

    /** Builds a profile containing both a cape and the requested skin model for preservation checks. */
    private String textureValue(SkinVariant variant) {
        String metadata = variant == SkinVariant.SLIM ? ",\"metadata\":{\"model\":\"slim\"}" : "";
        String texture = """
                {"textures":{"SKIN":{"url":"https://textures.minecraft.net/texture/skin"%s},
                "CAPE":{"url":"https://textures.minecraft.net/texture/cape"}}}
                """.formatted(metadata);
        return Base64.getEncoder().encodeToString(texture.getBytes(StandardCharsets.UTF_8));
    }

    private void respond(HttpResponse first, HttpResponse... remaining) throws Exception {
        when(settings.getProperty(APIConfig.MINESKIN_API_KEY)).thenReturn("");
        when(settings.getProperty(APIConfig.MINESKIN_SECRET_SKINS)).thenReturn(false);
        when(httpClient.execute(any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(first, remaining);
    }
}
