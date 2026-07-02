/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
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
package net.skinsrestorer.shared.skinsafety;

import ch.jalu.configme.SettingsManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.config.SkinSafetyConfig;
import net.skinsrestorer.shared.connections.http.HttpClient;
import net.skinsrestorer.shared.connections.requests.SkinSafetyReportRequest;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinSafetyReportService {
    private static final Gson GSON = new GsonBuilder().create();

    private final SettingsManager settings;
    private final SRLogger logger;
    private final SRPlugin plugin;
    private final SRPlatformAdapter adapter;
    private final HttpClient httpClient;
    private final SkinSafetyState state;

    public void report(SkinSafetyReportRequest request) {
        appendLocalReport(request);

        String remoteUrl = settings.getProperty(SkinSafetyConfig.REPORT_REMOTE_URL).trim();
        if (remoteUrl.isEmpty()) {
            return;
        }

        adapter.runAsync(() -> sendRemoteReport(remoteUrl, request));
    }

    public Path getLocalReportPath() {
        return state.getSkinSafetyFolder().resolve("reports.jsonl");
    }

    private void appendLocalReport(SkinSafetyReportRequest request) {
        try {
            Files.createDirectories(state.getSkinSafetyFolder());
            Files.writeString(
                    getLocalReportPath(),
                    GSON.toJson(request) + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            logger.warning("Failed to write local skin safety report: %s".formatted(e.getMessage()));
        }
    }

    private void sendRemoteReport(String remoteUrl, SkinSafetyReportRequest request) {
        try {
            httpClient.execute(
                    java.net.URI.create(remoteUrl),
                    new HttpClient.RequestBody(GSON.toJson(request), HttpClient.HttpType.JSON),
                    HttpClient.HttpType.JSON,
                    plugin.getUserAgent(),
                    HttpClient.HttpMethod.POST,
                    Map.of(),
                    20_000
            );
        } catch (Exception e) {
            logger.warning("Failed to submit remote skin safety report: %s".formatted(e.getMessage()));
        }
    }
}
