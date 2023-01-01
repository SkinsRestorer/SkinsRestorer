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
package net.skinsrestorer.shared.utils.connections;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.interfaces.SRPlugin;
import net.skinsrestorer.shared.serverinfo.ServerInfo;
import net.skinsrestorer.shared.utils.connections.requests.DumpInfo;
import net.skinsrestorer.shared.utils.log.SRLogger;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DumpService {
    private final SRLogger logger;
    private final SRPlugin plugin;
    private final Gson gson = new Gson();

    public Optional<String> dump() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("https://bytebin.lucko.me/post").openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "SkinsRestorer/" + plugin.getVersion());

        DumpInfo dumpInfo = new DumpInfo(
                plugin.getVersion(),
                plugin.getProxyModeInfo(),
                plugin.getPlatformVersion(),
                ServerInfo.determineEnvironment(plugin.getPlatform())
        );
        try (OutputStream os = connection.getOutputStream()) {
            os.write(gson.toJson(dumpInfo).getBytes(StandardCharsets.UTF_8));
        }

        if (connection.getResponseCode() != 201) {
            logger.warning("Failed to dump data to bytebin. Response code: " + connection.getResponseCode());
            return Optional.empty();
        }

        return connection.getHeaderField("Location").describeConstable();
    }
}
