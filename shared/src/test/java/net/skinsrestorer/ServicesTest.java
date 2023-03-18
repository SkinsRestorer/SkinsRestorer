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

import ch.jalu.injector.Injector;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.connections.ServiceCheckerService;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.serverinfo.Platform;
import net.skinsrestorer.shared.update.SharedUpdateCheckInit;
import net.skinsrestorer.shared.utils.MetricsCounter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith({MockitoExtension.class, SRExtension.class})
public class ServicesTest {
    @Mock
    private SRPlatformAdapter<?> srPlatformAdapter;
    @Mock
    private SkinsRestorerLocale skinsRestorerLocale;

    @Test
    public void testServices(Injector injector) {
        injector.register(SkinsRestorerLocale.class, skinsRestorerLocale);
        injector.register(SRPlatformAdapter.class, srPlatformAdapter);

        new SRPlugin(injector, "UnitTest", null, Platform.BUKKIT, SharedUpdateCheckInit.class);

        MetricsCounter metricsCounter = injector.getSingleton(MetricsCounter.class);
        ServiceCheckerService.ServiceCheckResponse serviceChecker = injector.getSingleton(ServiceCheckerService.class).checkServices();

        serviceChecker.getResults().forEach(System.out::println);

        assertFalse(serviceChecker.getResults().isEmpty());
        assertEquals(3, serviceChecker.getWorkingUUID());
        assertEquals(3, serviceChecker.getWorkingProfile());

        assertEquals(2, metricsCounter.collect(MetricsCounter.Service.ASHCON));
        assertEquals(2, metricsCounter.collect(MetricsCounter.Service.MINE_TOOLS));
        assertEquals(2, metricsCounter.collect(MetricsCounter.Service.MOJANG));
    }
}
