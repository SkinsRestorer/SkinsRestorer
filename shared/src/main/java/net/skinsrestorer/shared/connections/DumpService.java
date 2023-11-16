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
package net.skinsrestorer.shared.connections;

import ch.jalu.injector.Injector;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.DataRequestException;
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
import java.util.Collections;
import java.util.Optional;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DumpService {
    private final SRLogger logger;
    private final SRPlugin plugin;
    private final SRPlatformAdapter<?> adapter;
    private final Injector injector;
    private final HttpClient httpClient;
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

        EnvironmentInfo environmentInfo = EnvironmentInfo.determineEnvironment(adapter);
        PlatformInfo platformInfo = new PlatformInfo(
                adapter.getPlatformName(),
                adapter.getPlatformVendor(),
                adapter.getPlatformVersion(),
                adapter.getPlugins()
        );

        DumpInfo dumpInfo = new DumpInfo(
                plugin.getVersion(),
                proxyMode,
                environmentInfo,
                platformInfo,
                new DumpInfo.OSInfo(),
                new DumpInfo.JavaInfo(),
                new DumpInfo.UserInfo()
        );

        HttpResponse response = httpClient.execute("https://bytebin.lucko.me/post",
                new HttpClient.RequestBody(gson.toJson(dumpInfo), HttpClient.HttpType.JSON),
                HttpClient.HttpType.JSON,
                plugin.getUserAgent(),
                HttpClient.HttpMethod.POST,
                Collections.emptyMap(),
                20_000
        );

        if (response.getStatusCode() != 201) {
            logger.warning("Failed to dump data to bytebin. Response code: " + response.getStatusCode());
            return Optional.empty();
        }

        BytebinResponse responseObject = response.getBodyAs(BytebinResponse.class);

        return Optional.of(responseObject.getKey());
    }
}
