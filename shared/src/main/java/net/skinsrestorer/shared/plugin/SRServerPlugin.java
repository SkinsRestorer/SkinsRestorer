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
package net.skinsrestorer.shared.plugin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.log.SRLogger;

import javax.inject.Inject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SRServerPlugin {
    private final SRPlugin plugin;
    private final SRServerAdapter<?> serverAdapter;
    private final SRLogger logger;
    @Getter
    @Setter
    private boolean proxyMode;

    @SuppressWarnings("unchecked")
    public static Map<String, String> convertToObjectV2(byte[] byteArr) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(byteArr)));

            return (Map<String, String>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    public void checkProxyMode() {
        proxyMode = checkProxy();

        try {
            final Path warning = plugin.getDataFolder().resolve("(README) Use proxy config for settings! (README).txt");
            if (proxyMode) {
                if (!Files.isDirectory(plugin.getDataFolder())) { // in case the directory is a symbol link
                    Files.createDirectories(plugin.getDataFolder());
                }

                try (final InputStream inputStream = serverAdapter.getResource("proxy_warning.txt")) {
                    if (inputStream == null) {
                        throw new IllegalStateException("Could not find proxy_warning.txt in resources!");
                    }

                    final ByteArrayOutputStream result = new ByteArrayOutputStream();
                    final byte[] buffer = new byte[8192];
                    for (int length; (length = inputStream.read(buffer)) != -1; ) {
                        result.write(buffer, 0, length);
                    }
                    final String proxyWarning = result.toString(StandardCharsets.UTF_8.name());

                    if (Files.exists(warning)) {
                        final String existingWarning = new String(Files.readAllBytes(warning), StandardCharsets.UTF_8);
                        if (!existingWarning.equals(proxyWarning)) {
                            Files.write(warning, proxyWarning.getBytes(StandardCharsets.UTF_8));
                        }
                    } else {
                        Files.copy(inputStream, warning);
                    }
                }
            } else {
                Files.deleteIfExists(warning);
            }
        } catch (IOException e) {
            e.printStackTrace();
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
        init.initPermissions();

        init.initGUIListener();

        if (proxyMode) {
            if (Files.exists(plugin.getDataFolder().resolve("enableSkinStorageAPI.txt"))) {
                plugin.loadStorage();
                plugin.registerAPI();
            }

            init.initMessageChannel();
        } else {
            plugin.loadStorage();

            // Init API
            plugin.registerAPI();

            // Init commands
            plugin.initCommands();

            // Init listener
            init.initLoginProfileListener();
        }
    }
}
