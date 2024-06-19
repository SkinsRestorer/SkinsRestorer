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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.shared.connections.responses.RecommenationResponse;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RecommendationsState {
    private final SRPlugin plugin;
    private final SRPlatformAdapter<?, ?> adapter;
    private final SRLogger logger;
    private final RecommendationsService recommendationsService;
    @Getter
    private RecommenationResponse.SkinInfo[] recommendations = new RecommenationResponse.SkinInfo[0];
    private final Gson gson = new GsonBuilder().create();

    public void scheduleRecommendations() {
        Path path = plugin.getDataFolder().resolve("recommendations.json");

        boolean fileExists = Files.exists(path);
        if (fileExists) {
            try {
                RecommenationResponse responseObject = gson.fromJson(Files.newBufferedReader(path), RecommenationResponse.class);
                this.recommendations = responseObject.getSkins();
            } catch (IOException e) {
                logger.warning("Failed to load recommendations from file: " + e.getMessage());
            }
        }

        var offsetSeconds = fileExists ? ThreadLocalRandom.current().nextInt(0, 300) : 0;
        adapter.runRepeatAsync(() -> {
            try {
                recommendationsService.getRecommendations().ifPresent(recommenationResponse -> {
                    this.recommendations = recommenationResponse.getSkins();

                    try {
                        Files.write(path, gson.toJson(recommenationResponse).getBytes());
                    } catch (IOException e) {
                        logger.warning("Failed to save recommendations to file: " + e.getMessage());
                    }
                });
            } catch (IOException | DataRequestException e) {
                logger.warning("Failed to get recommended skins: " + e.getMessage());
            }
        }, offsetSeconds, (int) (TimeUnit.HOURS.toSeconds(6) + offsetSeconds), TimeUnit.SECONDS);
    }
}
