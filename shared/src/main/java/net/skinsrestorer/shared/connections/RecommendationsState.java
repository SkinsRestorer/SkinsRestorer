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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.shared.config.APIConfig;
import net.skinsrestorer.shared.config.GUIConfig;
import net.skinsrestorer.shared.connections.responses.RecommenationResponse;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.utils.SRHelpers;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RecommendationsState {
    private final SRPlugin plugin;
    private final SRPlatformAdapter adapter;
    private final SRLogger logger;
    private final RecommendationsService recommendationsService;
    private final SettingsManager settingsManager;
    private final Gson gson = new GsonBuilder().create();
    private Map<String, RecommenationResponse.SkinInfo> recommendationsMap = Map.of();
    private List<RecommenationResponse.SkinInfo> recommendationsList = List.of();

    public void scheduleRecommendations() {
        if (!settingsManager.getProperty(APIConfig.FETCH_RECOMMENDED_SKINS)) {
            logger.info("Fetching recommended skins is disabled.");
            return;
        }

        Path path = plugin.getDataFolder().resolve("recommendations.json");

        boolean fileExists = Files.exists(path);
        if (fileExists) {
            try (Reader reader = Files.newBufferedReader(path)) {
                RecommenationResponse recommenationResponse = gson.fromJson(reader, RecommenationResponse.class);
                if (recommenationResponse == null || recommenationResponse.getSkins() == null) {
                    throw new IOException("Invalid data");
                }

                setDataFromResponse(recommenationResponse.getSkins());
            } catch (IOException e) {
                logger.warning("Failed to load recommendations from file: %s".formatted(e.getMessage()));
            }
        }

        var offsetSeconds = fileExists ? ThreadLocalRandom.current().nextInt(0, 300) : 0;
        adapter.runRepeatAsync(() -> {
            try {
                recommendationsService.getRecommendations().ifPresent(recommenationResponse -> {
                    setDataFromResponse(recommenationResponse.getSkins());

                    try {
                        SRHelpers.writeIfNeeded(path, gson.toJson(recommenationResponse));
                    } catch (IOException e) {
                        logger.warning("Failed to save recommendations to file: %s".formatted(e.getMessage()));
                    }
                });
            } catch (IOException | DataRequestException e) {
                logger.warning("Failed to get recommended skins: %s".formatted(e.getMessage()));
            }
        }, offsetSeconds, (int) (TimeUnit.HOURS.toSeconds(6) + offsetSeconds), TimeUnit.SECONDS);
    }

    private void setDataFromResponse(RecommenationResponse.SkinInfo[] recommendations) {
        recommendationsMap = Stream.of(recommendations).collect(Collectors.toMap(RecommenationResponse.SkinInfo::getSkinId, skinInfo -> skinInfo));
        recommendationsList = Arrays.asList(recommendations);
        Collections.shuffle(recommendationsList);
    }

    public int getRecommendationsCount() {
        return getRecommendationsOffset(0, Integer.MAX_VALUE).length;
    }

    public RecommenationResponse.SkinInfo[] getRecommendationsOffset(int offset, int limit) {
        return recommendationsList.stream()
                .filter(skinInfo -> {
                    if (!settingsManager.getProperty(GUIConfig.RECOMMENDATIONS_GUI_ONLY_LIST)) {
                        return true;
                    }

                    return settingsManager.getProperty(GUIConfig.RECOMMENDATIONS_GUI_LIST).contains(skinInfo.getSkinId());
                })
                .skip(offset)
                .limit(limit)
                .toArray(RecommenationResponse.SkinInfo[]::new);
    }

    public Optional<RecommenationResponse.SkinInfo> getRandomRecommendation() {
        if (recommendationsList.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(SRHelpers.getRandomEntry(recommendationsList));
    }

    public RecommenationResponse.SkinInfo getRecommendation(String skinId) {
        return recommendationsMap.get(skinId);
    }
}
