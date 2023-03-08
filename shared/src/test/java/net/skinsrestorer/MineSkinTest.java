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
package net.skinsrestorer;

import ch.jalu.configme.SettingsManager;
import ch.jalu.injector.Injector;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.config.APIConfig;
import net.skinsrestorer.shared.connections.MineSkinAPIImpl;
import net.skinsrestorer.shared.utils.MetricsCounter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, SRExtension.class})
public class MineSkinTest {
    @Mock
    private SettingsManager settingsManager;
    @Mock
    private SkinsRestorerLocale skinsRestorerLocale;

    private static final String TEST_URL = "https://skinsrestorer.net/skinsrestorer-skin.png";

    @Test
    public void testServices(Injector injector) throws DataRequestException {
        injector.register(SkinsRestorerLocale.class, skinsRestorerLocale);

        when(settingsManager.getProperty(APIConfig.MINESKIN_API_KEY)).thenReturn("");

        injector.register(SettingsManager.class, settingsManager);

        MetricsCounter metricsCounter = injector.getSingleton(MetricsCounter.class);
        SkinProperty skinProperty = injector.getSingleton(MineSkinAPIImpl.class).genSkin(TEST_URL, null);

        assertNotNull(skinProperty);

        assertEquals(1, metricsCounter.collect(MetricsCounter.Service.MINE_SKIN));
    }
}
