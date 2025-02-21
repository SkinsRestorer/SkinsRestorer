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
package net.skinsrestorer.bukkit.hooks;

import ch.jalu.injector.Injector;
import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.bukkit.utils.SkinApplyBukkitAdapter;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.storage.HardcodedSkins;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
public class SRPlaceholderAPIExpansion extends PlaceholderExpansion {
    private static final String ERROR_MESSAGE = "Error";
    private final SRLogger logger;
    private final PluginDescriptionFile description;
    private final Injector injector;

    @Override
    public @NotNull String getIdentifier() {
        return "skinsrestorer";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", description.getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return description.getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        params = params.toLowerCase(Locale.ROOT);

        if (params.startsWith("skin_name")) {
            if (offlinePlayer == null) {
                return ERROR_MESSAGE;
            }

            Optional<SkinIdentifier> skin = SkinsRestorerProvider.get()
                    .getPlayerStorage()
                    .getSkinIdOfPlayer(offlinePlayer.getUniqueId());

            if (skin.isPresent()) {
                return skin.get().getIdentifier();
            }

            if (params.startsWith("skin_name_")) {
                String subString = params.substring("skin_name_".length());

                if (subString.equalsIgnoreCase("or_empty")) {
                    return "";
                } else if (subString.equalsIgnoreCase("or_player_name")) {
                    return offlinePlayer.getName();
                }
            }

            return ERROR_MESSAGE;
        } else if (params.startsWith("texture_url")) {
            if (offlinePlayer == null) {
                return ERROR_MESSAGE;
            }

            try {
                Optional<SkinProperty> skin = getCurrentProperties(offlinePlayer);

                if (skin.isPresent()) {
                    return extractTextureUrl(skin.get());
                }

                if (params.startsWith("texture_url_")) {
                    String subString = params.substring("texture_url_".length());

                    if (subString.equalsIgnoreCase("or_empty")) {
                        return "";
                    } else if (subString.equalsIgnoreCase("or_steve")) {
                        return extractTextureUrl(HardcodedSkins.STEVE.getProperty());
                    } else if (subString.equalsIgnoreCase("or_alex")) {
                        return extractTextureUrl(HardcodedSkins.ALEX.getProperty());
                    }
                }
            } catch (DataRequestException e) {
                logger.severe("Failed to get skin data of player %s".formatted(offlinePlayer.getUniqueId()), e);
            }

            return ERROR_MESSAGE;
        } else if (params.startsWith("texture_id")) {
            if (offlinePlayer == null) {
                return ERROR_MESSAGE;
            }

            try {
                Optional<SkinProperty> skin = getCurrentProperties(offlinePlayer);

                if (skin.isPresent()) {
                    return extractTextureHash(skin.get());
                }

                if (params.startsWith("texture_id_")) {
                    String subString = params.substring("texture_id_".length());

                    if (subString.equalsIgnoreCase("or_empty")) {
                        return "";
                    } else if (subString.equalsIgnoreCase("or_steve")) {
                        return extractTextureHash(HardcodedSkins.STEVE.getProperty());
                    } else if (subString.equalsIgnoreCase("or_alex")) {
                        return extractTextureHash(HardcodedSkins.ALEX.getProperty());
                    }
                }
            } catch (DataRequestException e) {
                logger.severe("Failed to get skin data of player %s".formatted(offlinePlayer.getUniqueId()), e);
            }

            return ERROR_MESSAGE;
        }

        return null;
    }

    private String extractTextureUrl(SkinProperty property) {
        return PropertyUtils.getSkinTextureUrl(property);
    }

    private String extractTextureHash(SkinProperty property) {
        return PropertyUtils.getSkinTextureHash(property);
    }

    private Optional<SkinProperty> getCurrentProperties(OfflinePlayer offlinePlayer) throws DataRequestException {
        if (offlinePlayer instanceof Player player) {
            return injector.getSingleton(SkinApplyBukkitAdapter.class).getSkinProperty(player);
        } else {
            return SkinsRestorerProvider.get()
                    .getPlayerStorage()
                    .getSkinForPlayer(offlinePlayer.getUniqueId(), offlinePlayer.getName());
        }
    }
}
