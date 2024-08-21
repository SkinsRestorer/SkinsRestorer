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
package net.skinsrestorer.shared.gui;

import ch.jalu.configme.SettingsManager;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.shared.config.GUIConfig;
import net.skinsrestorer.shared.storage.PlayerStorageImpl;
import net.skinsrestorer.shared.storage.model.player.FavouriteData;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.messages.ComponentString;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.subjects.permissions.SkinPermissionManager;
import net.skinsrestorer.shared.utils.SRHelpers;

import java.util.*;

public class GUIUtils {
    public static PageInfo getGUIPage(SRPlayer player,
                                      SkinsRestorerLocale locale,
                                      SettingsManager settings,
                                      PlayerStorageImpl playerStorage,
                                      SkinPermissionManager permissionManager,
                                      int page, PageType pageType, GUIDataSource... sources) {
        if (page < 0) {
            page = 0;
        }

        List<GUIDataSource> enabledSources = Arrays.stream(sources)
                .filter(GUIDataSource::isEnabled)
                .filter(source -> source.getPageType() == pageType)
                .sorted(Comparator.comparingInt(GUIDataSource::getIndex))
                .toList();
        int offset = page * SharedGUI.HEAD_COUNT_PER_PAGE;
        List<GUISkinEntry> skinPage = new ArrayList<>(SharedGUI.HEAD_COUNT_PER_PAGE);

        boolean hasNextPage = false;
        int currentIndex = 0;
        for (GUIDataSource source : enabledSources) {
            int sourceTotal = source.getTotalSkins();
            if (currentIndex + sourceTotal <= offset) {
                currentIndex += sourceTotal;
                continue;
            }

            int sourceOffset = offset - currentIndex;
            int sourceLimit = SharedGUI.HEAD_COUNT_PER_PAGE - skinPage.size();
            if (sourceOffset < 0) {
                sourceLimit += sourceOffset;
                sourceOffset = 0;
            }

            List<GUIUtils.GUIRawSkinEntry> sourceSkins = source.getGUISkins(sourceOffset, sourceLimit);
            sourceSkins.stream()
                    .map(base -> {
                        List<ComponentString> lore = new ArrayList<>();
                        String textureHash;
                        boolean canSetSkin = permissionManager.canSetSkin(player, base.skinIdentifier().getIdentifier()).isEmpty();
                        if (canSetSkin) {
                            textureHash = base.textureHash();
                        } else {
                            textureHash = settings.getProperty(GUIConfig.NOT_UNLOCKED_SKIN);
                        }

                        Optional<FavouriteData> favouriteData;
                        if (canSetSkin) {
                            lore.add(locale.getMessageRequired(player, Message.SKINSMENU_SELECT_SKIN));
                            favouriteData = playerStorage.getFavouriteData(player.getUniqueId(), base.skinIdentifier());
                            if (favouriteData.isPresent()) {
                                lore.add(locale.getMessageRequired(player, Message.SKINSMENU_REMOVE_FAVOURITE_LORE));
                            } else {
                                lore.add(locale.getMessageRequired(player, Message.SKINSMENU_SET_FAVOURITE_LORE));
                            }
                            favouriteData.ifPresent(data -> lore.add(locale.getMessageRequired(player, Message.SKINSMENU_FAVOURITE_SINCE_LORE,
                                    Placeholder.unparsed("time", SRHelpers.formatEpochSeconds(settings, data.getTimestamp(), player.getLocale())))));

                            lore.addAll(base.extraLore());
                        } else {
                            favouriteData = Optional.empty();
                            lore.add(locale.getMessageRequired(player, Message.SKINSMENU_NO_PERMISSION));
                        }

                        return new GUISkinEntry(base.skinIdentifier(), base.skinName(), textureHash, lore, favouriteData.isPresent());
                    })
                    .forEach(skinPage::add);

            if (sourceSkins.size() < sourceTotal - sourceOffset) {
                hasNextPage = true;
                break;
            }
        }

        return new PageInfo(
                page,
                pageType,
                page > 0,
                page < Integer.MAX_VALUE && hasNextPage,
                skinPage
        );
    }

    public interface GUIDataSource {
        boolean isEnabled();

        PageType getPageType();

        int getIndex();

        int getTotalSkins();

        List<GUIUtils.GUIRawSkinEntry> getGUISkins(int offset, int limit);
    }

    public record GUIRawSkinEntry(SkinIdentifier skinIdentifier, ComponentString skinName, String textureHash,
                                  List<ComponentString> extraLore) {
    }
}
