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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.log.SRLogger;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SRServerPlugin {
    private final SRPlugin plugin;
    private final SRServerAdapter serverAdapter;
    private final SRLogger logger;
    @Getter
    @Setter
    private boolean proxyMode;

    public void checkProxyMode() {
        proxyMode = checkProxy();

        try {
            Path warning = plugin.getDataFolder().resolve("(README) Use proxy config for settings! (README).txt");
            if (proxyMode) {
                if (!Files.isDirectory(plugin.getDataFolder())) { // in case the directory is a symbol link
                    Files.createDirectories(plugin.getDataFolder());
                }

                try (InputStream inputStream = serverAdapter.getResource("proxy_warning.txt")) {
                    if (inputStream == null) {
                        throw new IllegalStateException("Could not find proxy_warning.txt in resources!");
                    }

                    String proxyWarning = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                            .lines()
                            .collect(Collectors.joining("\n"));

                    if (Files.exists(warning)) {
                        String existingWarning = Files.readString(warning);
                        if (!existingWarning.equals(proxyWarning)) {
                            Files.writeString(warning, proxyWarning);
                        }
                    } else {
                        Files.copy(inputStream, warning);
                    }
                }
            } else {
                Files.deleteIfExists(warning);
            }
        } catch (IOException e) {
            logger.severe("Failed to create proxy warning file", e);
        }

        if (proxyMode) {
            logger.info("-------------------------/Warning\\-------------------------");
            logger.info("This plugin is running in PROXY mode!");
            logger.info("You have to do all configuration at config file");
            logger.info("inside your BungeeCord/Velocity server.");
            logger.info("(<proxy>/plugins/SkinsRestorer/)");
            logger.info("-------------------------\\Warning/-------------------------");
        }
    }

    private boolean checkProxy() {
        Path proxyModeEnabled = plugin.getDataFolder().resolve("enableProxyMode.txt");
        Path proxyModeDisabled = plugin.getDataFolder().resolve("disableProxyMode.txt");

        if (Files.exists(proxyModeEnabled)) {
            return true;
        }

        if (Files.exists(proxyModeDisabled)) {
            return false;
        }

        return serverAdapter.determineProxy();
    }

    public void startupPlatform(SRServerPlatformInit init) throws InitializeException {
        init.initMetricsJoinListener();

        init.initPermissions();

        init.initGUIListener();

        init.initAdminInfoListener();

        if (proxyMode) {
            if (Files.exists(plugin.getDataFolder().resolve("enableSkinStorageAPI.txt"))) {
                plugin.loadStorage();
                plugin.registerAPI();

                // Load Floodgate hook
                plugin.registerFloodgate();
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
