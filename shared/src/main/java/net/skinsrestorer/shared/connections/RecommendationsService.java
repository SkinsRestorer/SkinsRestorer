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

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.shared.connections.http.HttpClient;
import net.skinsrestorer.shared.connections.http.HttpResponse;
import net.skinsrestorer.shared.connections.responses.RecommenationResponse;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlugin;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Optional;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RecommendationsService {
    private static final URI RECOMMENDATIONS_API = URI.create("https://cool-skins.skinsrestorer.net/list.json");

    private final SRLogger logger;
    private final SRPlugin plugin;
    private final HttpClient httpClient;

    public Optional<RecommenationResponse> getRecommendations() throws IOException, DataRequestException {
        HttpResponse response = httpClient.execute(
                RECOMMENDATIONS_API,
                null,
                HttpClient.HttpType.JSON,
                plugin.getUserAgent(),
                HttpClient.HttpMethod.GET,
                Collections.emptyMap(),
                20_000
        );

        if (response.statusCode() != 200) {
            logger.warning("Failed to get recommended skins. Response code: %d".formatted(response.statusCode()));
            return Optional.empty();
        }

        RecommenationResponse responseObject = response.getBodyAs(RecommenationResponse.class);

        return Optional.of(responseObject);
    }
}
