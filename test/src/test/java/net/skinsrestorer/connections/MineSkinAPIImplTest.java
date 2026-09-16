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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MineSkinAPIImplTest {
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

    private void respond(HttpResponse first, HttpResponse... remaining) throws Exception {
        when(settings.getProperty(APIConfig.MINESKIN_API_KEY)).thenReturn("");
        when(settings.getProperty(APIConfig.MINESKIN_SECRET_SKINS)).thenReturn(false);
        when(httpClient.execute(any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(first, remaining);
    }
}
