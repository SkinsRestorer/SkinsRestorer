/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
package net.skinsrestorer.shared.utils;

import ch.jalu.configme.SettingsManager;
import ch.jalu.injector.Injector;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.config.DatabaseConfig;
import net.skinsrestorer.shared.plugin.SRServerPlugin;

import javax.inject.Inject;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MetricsCounter {
    private final Injector injector;
    private final SettingsManager settingsManager;
    private final Map<Service, AtomicInteger> map = new EnumMap<>(Service.class);

    public String usesMySQL() {
        return String.valueOf(settingsManager.getProperty(DatabaseConfig.MYSQL_ENABLED));
    }

    public String isProxyMode() {
        SRServerPlugin serverPlugin = injector.getIfAvailable(SRServerPlugin.class);

        if (serverPlugin == null) {
            return null;
        }

        return String.valueOf(serverPlugin.isProxyMode());
    }

    public void increment(Service service) {
        getOrCreate(service).incrementAndGet();
    }

    public int collect(Service service) {
        return getOrCreate(service).getAndSet(0);
    }

    private AtomicInteger getOrCreate(Service service) {
        return map.computeIfAbsent(service, (k) -> new AtomicInteger());
    }

    public enum Service {
        MINE_SKIN,
        MINE_TOOLS,
        MOJANG,
        ASHCON
    }
}
