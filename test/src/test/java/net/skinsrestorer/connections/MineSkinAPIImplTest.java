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
import net.skinsrestorer.api.property.SkinVariant;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.exception.MineSkinException;
import net.skinsrestorer.shared.config.APIConfig;
import net.skinsrestorer.shared.connections.MineSkinAPIImpl;
import net.skinsrestorer.shared.connections.http.HttpClient;
import net.skinsrestorer.shared.connections.http.HttpResponse;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.utils.MetricsCounter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

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
    private final MineSkinAPIImpl api = new MineSkinAPIImpl(mock(SRLogger.class), mock(MetricsCounter.class),
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
            "{\"success\":true,\"skin\":{\"uuid\":\"id\",\"texture\":{\"data\":{\"value\":\"unsigned\"}}}}"})
    void invalidLookupResponsesAreRequestFailures(String body) throws Exception {
        respond(decrypted("https://minesk.in/" + SKIN_ID), new HttpResponse(200, body, Map.of()));

        assertThrows(DataRequestException.class, () -> api.genSkin(AXOLOTL_URL, null));
    }

    @Test
    void decryptConnectionFailureNeverReachesMineSkin() throws Exception {
        when(httpClient.execute(any(), any(), any(), any(), any(), any(), anyInt()))
                .thenThrow(new IOException("Connection failed"));

        assertThrows(DataRequestException.class, () -> api.genSkin(AXOLOTL_URL, null));
        verify(httpClient, times(1)).execute(any(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void lookupConnectionFailureNeverFallsBackToGeneration() throws Exception {
        respond(decrypted("https://minesk.in/" + SKIN_ID));
        when(httpClient.execute(eq(LOOKUP_URI), any(), any(), any(), any(), any(), anyInt()))
                .thenThrow(new IOException("Connection failed"));

        assertThrows(DataRequestException.class, () -> api.genSkin(AXOLOTL_URL, null));
        verify(httpClient, times(2)).execute(any(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void normalImageUrlsStillUseGeneration() throws Exception {
        String value = textureValue(SkinVariant.SLIM);
        respond(signedSkin(value));

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
                {"success":true,"skin":{"uuid":"%s","texture":{"data":{
                "value":"%s","signature":"original-signature"}}}}
                """.formatted(SKIN_ID, value), Map.of());
    }

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
