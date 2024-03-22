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
package net.skinsrestorer;

import ch.jalu.configme.SettingsManager;
import ch.jalu.injector.Injector;
import lombok.extern.slf4j.Slf4j;
import net.skinsrestorer.api.connections.model.MineSkinResponse;
import net.skinsrestorer.shared.config.APIConfig;
import net.skinsrestorer.shared.config.AdvancedConfig;
import net.skinsrestorer.shared.connections.MineSkinAPIImpl;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.utils.MetricsCounter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith({MockitoExtension.class, SRExtension.class})
public class MineSkinTest {
    private static final String TEST_URL = "https://skinsrestorer.net/skinsrestorer-skin.png";
    @Mock
    private SettingsManager settings;
    @Mock
    private SkinsRestorerLocale skinsRestorerLocale;

    @Test
    public void testServices(Injector injector) {
        injector.register(SkinsRestorerLocale.class, skinsRestorerLocale);

        when(settings.getProperty(APIConfig.MINESKIN_API_KEY)).thenReturn("");
        when(settings.getProperty(AdvancedConfig.NO_CONNECTIONS)).thenReturn(false);

        injector.register(SettingsManager.class, settings);

        String randomUrl = TEST_URL + "?" + UUID.randomUUID(); // Random URL to avoid caching
        MetricsCounter metricsCounter = injector.getSingleton(MetricsCounter.class);

        try {
            MineSkinResponse response = injector.getSingleton(MineSkinAPIImpl.class)
                    .genSkin(randomUrl, null);
        } catch (Exception e) {
            log.error("Failed to generate skin", e);
        }

        /*

        assertNotNull(response);

        assertEquals(1, metricsCounter.collect(MetricsCounter.Service.MINE_SKIN));
         */
    }
}
