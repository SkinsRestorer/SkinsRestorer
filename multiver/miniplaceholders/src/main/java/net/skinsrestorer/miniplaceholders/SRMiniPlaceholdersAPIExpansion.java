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
package net.skinsrestorer.miniplaceholders;

import io.github.miniplaceholders.api.Expansion;
import io.github.miniplaceholders.api.utils.Tags;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.builddata.BuildData;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.storage.HardcodedSkins;
import net.skinsrestorer.shared.subjects.SRPlayer;

import java.util.Optional;
import java.util.function.Function;

@RequiredArgsConstructor
public class SRMiniPlaceholdersAPIExpansion<P> {
    private final SRPlatformAdapter adapter;
    private final Class<P> playerClassRaw;
    private final Function<P, SRPlayer> playerProvider;

    @SuppressWarnings("unchecked")
    public void register() {
        var playerClass = (Class<? extends Audience>) playerClassRaw;
        Expansion.builder("skinsrestorer")
                .version(BuildData.VERSION)
                .author("SRTeam")
                .audiencePlaceholder(playerClass, "skin_name_or_empty", ((audience, queue, ctx) -> {
                    SRPlayer player = playerProvider.apply((P) audience);

                    Optional<SkinIdentifier> skin = SkinsRestorerProvider.get()
                            .getPlayerStorage()
                            .getSkinIdOfPlayer(player.getUniqueId());

                    if (skin.isPresent()) {
                        return Tag.preProcessParsed(skin.get().getIdentifier());
                    }

                    return Tags.EMPTY_TAG;
                }))
                .audiencePlaceholder(playerClass, "skin_name_or_player_name", ((audience, queue, ctx) -> {
                    SRPlayer player = playerProvider.apply((P) audience);

                    Optional<SkinIdentifier> skin = SkinsRestorerProvider.get()
                            .getPlayerStorage()
                            .getSkinIdOfPlayer(player.getUniqueId());

                    return skin.map(skinIdentifier -> Tag.preProcessParsed(skinIdentifier.getIdentifier()))
                            .orElseGet(() -> Tag.preProcessParsed(player.getName()));
                }))
                .audiencePlaceholder(playerClass, "texture_url_or_empty", ((audience, queue, ctx) -> {
                    SRPlayer player = playerProvider.apply((P) audience);

                    return adapter.getSkinProperty(player).map(this::extractTextureUrl).orElse(Tags.EMPTY_TAG);
                }))
                .audiencePlaceholder(playerClass, "texture_url_or_steve", ((audience, queue, ctx) -> {
                    SRPlayer player = playerProvider.apply((P) audience);

                    return adapter.getSkinProperty(player).map(this::extractTextureUrl).orElseGet(() -> extractTextureUrl(HardcodedSkins.STEVE.getProperty()));
                }))
                .audiencePlaceholder(playerClass, "texture_url_or_alex", ((audience, queue, ctx) -> {
                    SRPlayer player = playerProvider.apply((P) audience);

                    return adapter.getSkinProperty(player).map(this::extractTextureUrl).orElseGet(() -> extractTextureUrl(HardcodedSkins.ALEX.getProperty()));
                }))
                .audiencePlaceholder(playerClass, "texture_id_or_empty", ((audience, queue, ctx) -> {
                    SRPlayer player = playerProvider.apply((P) audience);

                    return adapter.getSkinProperty(player).map(this::extractTextureHash).orElse(Tags.EMPTY_TAG);
                }))
                .audiencePlaceholder(playerClass, "texture_id_or_steve", ((audience, queue, ctx) -> {
                    SRPlayer player = playerProvider.apply((P) audience);

                    return adapter.getSkinProperty(player).map(this::extractTextureHash).orElseGet(() -> extractTextureHash(HardcodedSkins.STEVE.getProperty()));
                }))
                .audiencePlaceholder(playerClass, "texture_id_or_alex", ((audience, queue, ctx) -> {
                    SRPlayer player = playerProvider.apply((P) audience);

                    return adapter.getSkinProperty(player).map(this::extractTextureHash).orElseGet(() -> extractTextureHash(HardcodedSkins.ALEX.getProperty()));
                }))
                .build()
                .register();
    }

    private Tag extractTextureUrl(SkinProperty property) {
        return Tag.preProcessParsed(PropertyUtils.getSkinTextureUrl(property));
    }

    private Tag extractTextureHash(SkinProperty property) {
        return Tag.preProcessParsed(PropertyUtils.getSkinTextureHash(property));
    }
}
