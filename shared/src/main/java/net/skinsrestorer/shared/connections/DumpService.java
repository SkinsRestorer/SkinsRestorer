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
package net.skinsrestorer.shared.connections;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerImpl;
import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.properties.Property;
import ch.jalu.injector.Injector;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.shared.config.APIConfig;
import net.skinsrestorer.shared.config.DatabaseConfig;
import net.skinsrestorer.shared.connections.http.HttpClient;
import net.skinsrestorer.shared.connections.http.HttpResponse;
import net.skinsrestorer.shared.connections.requests.DumpInfo;
import net.skinsrestorer.shared.connections.responses.BytebinResponse;
import net.skinsrestorer.shared.info.EnvironmentInfo;
import net.skinsrestorer.shared.info.PlatformInfo;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.plugin.SRServerPlugin;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DumpService {
    private static final URI BYTEBIN_ENDPOINT = URI.create("https://bytebin.lucko.me/post");
    private static final Function<SettingsManager, ConfigurationData> DATA_EXTRACTOR;

    static {
        // In static initializer so we skip a field lookup + can easier detect if the field changed
        try {
            Field field = SettingsManagerImpl.class.getDeclaredField("configurationData");
            field.setAccessible(true);

            DATA_EXTRACTOR = settingsManager -> {
                try {
                    return (ConfigurationData) field.get(settingsManager);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final SRLogger logger;
    private final SRPlugin plugin;
    private final SRPlatformAdapter adapter;
    private final Injector injector;
    private final HttpClient httpClient;
    private final SettingsManager settingsManager;
    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .create();

    public Optional<String> dump() throws IOException, DataRequestException {
        Boolean proxyMode;
        SRServerPlugin serverPlugin = injector.getIfAvailable(SRServerPlugin.class);
        if (serverPlugin == null) {
            proxyMode = null;
        } else {
            proxyMode = serverPlugin.isProxyMode();
        }

        ConfigurationData configurationData = DATA_EXTRACTOR.apply(settingsManager);
        JsonObject configMap = new JsonObject();
        for (Property<?> key : configurationData.getProperties()) {
            // Exclude sensitive data
            if (key.getPath().startsWith("database.")
                    || key.getPath().equals(APIConfig.MINESKIN_API_KEY.getPath())) {
                continue;
            }

            String[] split = key.getPath().split("\\.");
            JsonObject jsonObject = configMap;
            String keyName = split[split.length - 1];
            if (split.length > 1) {
                for (int i = 0; i < split.length - 1; i++) {
                    if (!jsonObject.has(split[i])) {
                        jsonObject.add(split[i], new JsonObject());
                    }

                    jsonObject = jsonObject.getAsJsonObject(split[i]);
                }
            }

            jsonObject.add(keyName, gson.toJsonTree(configurationData.getValue(key)));
        }

        DumpInfo.PluginInfo.StorageType storageType;
        if (proxyMode != null && proxyMode) {
            storageType = DumpInfo.PluginInfo.StorageType.NONE;
        } else {
            DatabaseConfig.DatabaseType databaseType = settingsManager.getProperty(DatabaseConfig.DATABASE_TYPE);
            storageType = switch (databaseType) {
                case FILE -> DumpInfo.PluginInfo.StorageType.FILE;
                case MYSQL -> DumpInfo.PluginInfo.StorageType.MYSQL;
                case POSTGRESQL -> DumpInfo.PluginInfo.StorageType.POSTGRESQL;
            };
        }

        DumpInfo.PluginInfo pluginInfo = new DumpInfo.PluginInfo(
                proxyMode,
                storageType,
                configMap
        );

        EnvironmentInfo environmentInfo = EnvironmentInfo.determineEnvironment(adapter);
        PlatformInfo platformInfo = new PlatformInfo(
                adapter.getPlatformName(),
                adapter.getPlatformVendor(),
                adapter.getPlatformVersion(),
                adapter.getPlugins()
        );

        DumpInfo dumpInfo = new DumpInfo(
                new DumpInfo.BuildInfo(),
                pluginInfo,
                environmentInfo,
                platformInfo,
                new DumpInfo.OSInfo(),
                new DumpInfo.JavaInfo(),
                new DumpInfo.UserInfo()
        );

        HttpResponse response = httpClient.execute(
                BYTEBIN_ENDPOINT,
                new HttpClient.RequestBody(gson.toJson(dumpInfo), HttpClient.HttpType.JSON),
                HttpClient.HttpType.JSON,
                plugin.getUserAgent(),
                HttpClient.HttpMethod.POST,
                Collections.emptyMap(),
                20_000
        );

        if (response.statusCode() != 201) {
            logger.warning("Failed to dump data to bytebin. Response code: %d".formatted(response.statusCode()));
            return Optional.empty();
        }

        BytebinResponse responseObject = response.getBodyAs(BytebinResponse.class);

        return Optional.of(responseObject.getKey());
    }
}
