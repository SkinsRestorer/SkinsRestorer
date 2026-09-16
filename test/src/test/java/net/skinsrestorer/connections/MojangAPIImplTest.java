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
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.config.APIConfig;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.connections.MojangAPIImpl;
import net.skinsrestorer.shared.connections.http.HttpClient;
import net.skinsrestorer.shared.connections.http.HttpResponse;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.utils.MetricsCounter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MojangAPIImplTest {
    private final HttpClient httpClient = mock(HttpClient.class);
    private final SettingsManager settings = mock(SettingsManager.class);
    private final MojangAPIImpl api = new MojangAPIImpl(mock(MetricsCounter.class), mock(SRLogger.class),
            mock(SRPlugin.class), httpClient, settings);

    @ParameterizedTest
    @ValueSource(ints = {204, 404})
    void missingProfilesDoNotFallBack(int status) throws Exception {
        when(settings.getProperty(APIConfig.ELYBY_ENABLED)).thenReturn(false);
        respond(new HttpResponse(status, "", Map.of()));

        assertTrue(api.getProfile(UUID.randomUUID()).isEmpty());
        verify(httpClient, times(1)).execute(any(), any(), any(), any(), any(), any(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(ints = {201, 301, 304, 400, 403, 429, 500, 503})
    void unexpectedStatusesTriggerFallback(int status) throws Exception {
        when(settings.getProperty(APIConfig.ELYBY_ENABLED)).thenReturn(false);
        respond(new HttpResponse(status, "{}", Map.of()), new HttpResponse(200,
                "{\"skinProperty\":{\"value\":\"texture\",\"signature\":\"signature\"}}", Map.of()));

        Optional<SkinProperty> result = api.getProfile(UUID.randomUUID());

        assertEquals(Optional.of(SkinProperty.of("texture", "signature")), result);
        verify(httpClient, times(2)).execute(any(), any(), any(), any(), any(), any(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(ints = {301, 429, 500})
    void freshProfilesPropagateUnexpectedStatuses(int status) throws Exception {
        when(settings.getProperty(APIConfig.ELYBY_ENABLED)).thenReturn(false);
        respond(new HttpResponse(status, "{}", Map.of()));

        assertThrows(DataRequestException.class, () -> api.getProfileFresh(UUID.randomUUID()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "{\"properties\":[]}"})
    void emptyProfileBodiesAreSafe(String body) throws Exception {
        respond(new HttpResponse(200, body, Map.of()));

        assertTrue(api.getProfileMojang(UUID.randomUUID()).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {204, 301, 400, 429, 500, 503})
    void batchFailuresReachTheCaller(int status) throws Exception {
        SRPlatformAdapter adapter = mock(SRPlatformAdapter.class);
        SRPlugin plugin = mock(SRPlugin.class);
        when(plugin.getAdapter()).thenReturn(adapter);
        when(settings.getProperty(APIConfig.MOJANG_BATCH_WINDOW_SECONDS)).thenReturn(0);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(adapter).runAsyncDelayed(any(Runnable.class), anyLong(), any(TimeUnit.class));
        MojangAPIImpl batchApi = new MojangAPIImpl(mock(MetricsCounter.class), mock(SRLogger.class),
                plugin, httpClient, settings);
        respond(new HttpResponse(status, "{}", Map.of()));

        assertThrows(DataRequestException.class, () -> batchApi.getUUIDMojang("Notch"));
    }

    private void respond(HttpResponse first, HttpResponse... remaining) throws Exception {
        when(httpClient.execute(any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(first, remaining);
    }
}
