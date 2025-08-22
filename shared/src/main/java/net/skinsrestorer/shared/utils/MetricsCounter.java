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
package net.skinsrestorer.shared.utils;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.properties.Property;
import ch.jalu.injector.Injector;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.config.DatabaseConfig;
import net.skinsrestorer.shared.plugin.SRServerPlugin;
import org.bstats.charts.SingleLineChart;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MetricsCounter {
    private final SettingsManager settings;
    private final Injector injector;
    private final SettingsManager settingsManager;
    private final Map<Service, AtomicInteger> serviceMap = new EnumMap<>(Service.class);
    private final Map<CommandType, AtomicInteger> commandeMap = new EnumMap<>(CommandType.class);


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

    public Map<String, Map<String, Integer>> pluginConfig() {
        Map<String, Map<String, Integer>> map = new HashMap<>();
        collectConfigDiff(map, "Advanced", net.skinsrestorer.shared.config.AdvancedConfig.class);
        collectConfigDiff(map, "Api", net.skinsrestorer.shared.config.AdvancedConfig.class);
        collectConfigDiff(map, "Command", net.skinsrestorer.shared.config.CommandConfig.class);
        collectConfigDiff(map, "Database", net.skinsrestorer.shared.config.DatabaseConfig.class);
        collectConfigDiff(map, "Dev", net.skinsrestorer.shared.config.DevConfig.class);
        collectConfigDiff(map, "GUI", net.skinsrestorer.shared.config.GUIConfig.class);
        collectConfigDiff(map, "Login", net.skinsrestorer.shared.config.LoginConfig.class);
        collectConfigDiff(map, "Message", net.skinsrestorer.shared.config.MessageConfig.class);
        collectConfigDiff(map, "Proxy", net.skinsrestorer.shared.config.ProxyConfig.class);
        collectConfigDiff(map, "Storage", net.skinsrestorer.shared.config.StorageConfig.class);
        collectConfigDiff(map, "Server", net.skinsrestorer.shared.config.ServerConfig.class);
        return map;
    }

    private void collectConfigDiff(Map<String, Map<String, Integer>> map, String name, Class<?> configClass) {
        Map<String, Integer> configMap = new HashMap<>();
        for (Field field : configClass.getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            if (!Property.class.isAssignableFrom(field.getType())) continue;
            try {
                Property<?> property = (Property<?>) field.get(null);
                Object value = settings.getProperty(property);
                Object defaultValue = property.getDefaultValue();
                boolean notDefault = !Objects.equals(value, defaultValue);
                configMap.put(property.getPath(), notDefault ? 1 : 0);
            } catch (IllegalAccessException ignored) {
            }
        }
        map.put(name, configMap);
    }

    public Map<String, Integer> skinCommand() {
        Map<String, Integer> map = new HashMap<>();
        for (MetricsCounter.CommandType commandType : MetricsCounter.CommandType.values()) {
            map.put(commandType.name().toLowerCase().replace("skin_", ""), collect(commandType));
        }
        return map;
    }

    public void increment(CommandType commandType) {
        getOrCreate(commandType).incrementAndGet();
    }

    public int collect(CommandType commandType) {
        return getOrCreate(commandType).getAndSet(0);
    }

    private AtomicInteger getOrCreate(CommandType commandType) {
        return commandeMap.computeIfAbsent(commandType, k -> new AtomicInteger());
    }

    public enum CommandType {
        SKINCOMMAND,
        SKIN_HELP,
        SKIN_SET,
        SKIN_SET_SHORT,
        SKIN_CLEAR,
        SKIN_RANDOM,
        SKIN_SEARCH,
        SKIN_EDIT,
        SKIN_UPDATE,
        SKIN_URL,
        SKIN_UNDO,
        SKIN_HISTORY,
        SKIN_FAVOURITE,
        SKIN_FAVOURITES,
        SKIN_GUI
    }

    public void increment(Service service) {
        getOrCreate(service).incrementAndGet();
    }

    public int collect(Service service) {
        return getOrCreate(service).getAndSet(0);
    }

    private AtomicInteger getOrCreate(Service service) {
        return serviceMap.computeIfAbsent(service, k -> new AtomicInteger());
    }

    public enum Service {
        MINE_SKIN,
        MOJANG_UUID,
        MOJANG_PROFILE,
        ECLIPSE_UUID,
        ECLIPSE_PROFILE
    }
}
