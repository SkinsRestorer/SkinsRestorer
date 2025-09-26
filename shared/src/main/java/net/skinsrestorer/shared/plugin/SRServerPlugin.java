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
package net.skinsrestorer.shared.plugin;

import ch.jalu.configme.SettingsManager;
import ch.jalu.injector.Injector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.skinsrestorer.shared.config.DatabaseConfig;
import net.skinsrestorer.shared.config.ServerConfig;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.log.SRLogger;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SRServerPlugin {
    private final SRPlugin plugin;
    private final SRServerAdapter serverAdapter;
    private final SRLogger logger;
    private final Injector injector;
    @Getter
    @Setter
    private boolean proxyMode;

    public void checkProxyMode() {
        proxyMode = checkProxy();

        try {
            Files.deleteIfExists(plugin.getDataFolder().resolve("(README) Use proxy config for settings! (README).txt"));
        } catch (IOException e) {
            logger.severe("Failed to create proxy warning file", e);
        }

        if (proxyMode) {
            logger.info("-------------------------/Warning\\-------------------------");
            logger.info("This plugin is running in PROXY mode!");
            logger.info("You have to put the same config.yml on all servers and on the proxy.");
            logger.info("(<proxy>/plugins/SkinsRestorer/)");
            logger.info("-------------------------\\Warning/-------------------------");
        }
    }

    private boolean checkProxy() {
        SettingsManager settingsManager = injector.getSingleton(SettingsManager.class);
        ServerConfig.ProxyMode proxyModeDetection = settingsManager.getProperty(ServerConfig.PROXY_MODE_DETECTION);
        return switch (proxyModeDetection) {
            case ENABLED -> true;
            case DISABLED -> false;
            case AUTO -> {
                Path proxyModeEnabled = plugin.getDataFolder().resolve("enableProxyMode.txt");
                Path proxyModeDisabled = plugin.getDataFolder().resolve("disableProxyMode.txt");

                if (Files.exists(proxyModeEnabled)) {
                    logger.warning("Proxy mode files are deprecated, please use the config file instead.");
                    yield true;
                }

                if (Files.exists(proxyModeDisabled)) {
                    logger.warning("Proxy mode files are deprecated, please use the config file instead.");
                    yield false;
                }

                yield serverAdapter.determineProxy();
            }
        };
    }

    public void startupPlatform(SRServerPlatformInit init) throws InitializeException {
        init.initMetricsJoinListener();

        init.initPermissions();

        init.initGUIListener();

        init.initAdminInfoListener();

        if (proxyMode) {
            boolean proxyModeApiFile = Files.exists(plugin.getDataFolder().resolve("enableSkinStorageAPI.txt"));
            if (proxyModeApiFile) {
                logger.warning("Proxy mode API files are deprecated, please use the config file instead.");
            }

            SettingsManager settingsManager = injector.getSingleton(SettingsManager.class);
            if (proxyModeApiFile || settingsManager.getProperty(ServerConfig.PROXY_MODE_API)) {
                DatabaseConfig.DatabaseType databaseType = settingsManager.getProperty(DatabaseConfig.DATABASE_TYPE);
                if (databaseType != DatabaseConfig.DatabaseType.FILE) {
                    plugin.loadStorage();
                    plugin.registerAPI();

                    // Load Floodgate hook
                    plugin.registerFloodgate();
                } else {
                    logger.warning("Proxy mode API is enabled (server.proxyMode.api), but database storage is not set up. Configure MySQL or PostgreSQL on all servers and on the proxy and use the same settings.");
                }
            }

            init.initMessageChannel();
        } else {
            plugin.loadStorage();

            // Init API
            plugin.registerAPI();

            // Load Floodgate hook
            plugin.registerFloodgate();

            // Init commands
            plugin.initCommands();

            // Init listener
            init.initLoginProfileListener();
        }

        init.placeholderSetupHook();
    }
}
