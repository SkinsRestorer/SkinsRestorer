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
package net.skinsrestorer.shared.storage;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shadow.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.shared.config.GUIConfig;
import net.skinsrestorer.shared.connections.RecommendationsState;
import net.skinsrestorer.shared.gui.GUIUtils;
import net.skinsrestorer.shared.gui.PageInfo;
import net.skinsrestorer.shared.gui.PageType;
import net.skinsrestorer.shared.storage.adapter.AdapterReference;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.messages.ComponentHelper;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;
import net.skinsrestorer.shared.subjects.permissions.SkinPermissionManager;
import net.skinsrestorer.shared.utils.SRHelpers;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class GUIStorage {
    public static final String RECOMMENDATION_PREFIX = "sr-recommendation-";
    private final SkinsRestorerLocale locale;
    private final PlayerStorageImpl playerStorage;
    private final SkinStorageImpl skinStorage;
    private final SettingsManager settings;
    private final AdapterReference adapterReference;
    private final RecommendationsState recommendationsState;
    private final SkinPermissionManager permissionManager;

    public PageInfo getGUIPage(SRPlayer player, int page, PageType pageType) {
        return GUIUtils.getGUIPage(player, locale, settings, playerStorage, permissionManager, page, pageType, new GUIUtils.GUIDataSource() {
            @Override
            public boolean isEnabled() {
                return settings.getProperty(GUIConfig.CUSTOM_GUI_ENABLED);
            }

            @Override
            public PageType getPageType() {
                return PageType.MAIN;
            }

            @Override
            public int getIndex() {
                return settings.getProperty(GUIConfig.CUSTOM_GUI_INDEX);
            }

            @Override
            public int getTotalSkins() {
                return adapterReference.get().getTotalCustomSkins();
            }

            @Override
            public List<GUIUtils.GUIRawSkinEntry> getGUISkins(int offset, int limit) {
                return adapterReference.get().getCustomGUISkins(offset, limit);
            }
        }, new GUIUtils.GUIDataSource() {
            @Override
            public boolean isEnabled() {
                return settings.getProperty(GUIConfig.PLAYERS_GUI_ENABLED);
            }

            @Override
            public PageType getPageType() {
                return PageType.MAIN;
            }

            @Override
            public int getIndex() {
                return settings.getProperty(GUIConfig.PLAYERS_GUI_INDEX);
            }

            @Override
            public int getTotalSkins() {
                return adapterReference.get().getTotalPlayerSkins();
            }

            @Override
            public List<GUIUtils.GUIRawSkinEntry> getGUISkins(int offset, int limit) {
                return adapterReference.get().getPlayerGUISkins(offset, limit);
            }
        }, new GUIUtils.GUIDataSource() {
            @Override
            public boolean isEnabled() {
                return settings.getProperty(GUIConfig.RECOMMENDATIONS_GUI_ENABLED);
            }

            @Override
            public PageType getPageType() {
                return PageType.MAIN;
            }

            @Override
            public int getIndex() {
                return settings.getProperty(GUIConfig.RECOMMENDATIONS_GUI_INDEX);
            }

            @Override
            public int getTotalSkins() {
                return recommendationsState.getRecommendationsCount();
            }

            @Override
            public List<GUIUtils.GUIRawSkinEntry> getGUISkins(int offset, int limit) {
                return Arrays.stream(recommendationsState.getRecommendationsOffset(offset, limit))
                        .map(r -> new GUIUtils.GUIRawSkinEntry(
                                SkinIdentifier.ofCustom(RECOMMENDATION_PREFIX + r.getSkinId()),
                                ComponentHelper.convertPlainToJson(r.getSkinName()),
                                PropertyUtils.getSkinTextureHash(r.getValue()),
                                List.of()
                        ))
                        .toList();
            }
        }, new GUIUtils.GUIDataSource() {
            @Override
            public boolean isEnabled() {
                return player.hasPermission(PermissionRegistry.SKIN_UNDO);
            }

            @Override
            public PageType getPageType() {
                return PageType.HISTORY;
            }

            @Override
            public int getIndex() {
                return 0;
            }

            @Override
            public int getTotalSkins() {
                return playerStorage.getHistoryCount(player.getUniqueId());
            }

            @Override
            public List<GUIUtils.GUIRawSkinEntry> getGUISkins(int offset, int limit) {
                return playerStorage.getHistoryEntries(player.getUniqueId(), offset, limit).stream()
                        .map(h -> new GUIUtils.GUIRawSkinEntry(
                                h.getSkinIdentifier(),
                                skinStorage.resolveSkinName(h.getSkinIdentifier()),
                                PropertyUtils.getSkinTextureHash(skinStorage.getSkinDataByIdentifier(h.getSkinIdentifier()).orElseThrow()),
                                List.of(locale.getMessageRequired(player, Message.SKINSMENU_HISTORY_LORE,
                                        Placeholder.parsed("time", SRHelpers.formatEpochSeconds(settings, h.getTimestamp(), player.getLocale()))))
                        ))
                        .toList();
            }
        }, new GUIUtils.GUIDataSource() {
            @Override
            public boolean isEnabled() {
                return player.hasPermission(PermissionRegistry.SKIN_FAVOURITE);
            }

            @Override
            public PageType getPageType() {
                return PageType.FAVOURITES;
            }

            @Override
            public int getIndex() {
                return 0;
            }

            @Override
            public int getTotalSkins() {
                return playerStorage.getFavouriteCount(player.getUniqueId());
            }

            @Override
            public List<GUIUtils.GUIRawSkinEntry> getGUISkins(int offset, int limit) {
                return playerStorage.getFavouriteEntries(player.getUniqueId(), offset, limit).stream()
                        .map(h -> new GUIUtils.GUIRawSkinEntry(
                                h.getSkinIdentifier(),
                                skinStorage.resolveSkinName(h.getSkinIdentifier()),
                                PropertyUtils.getSkinTextureHash(skinStorage.getSkinDataByIdentifier(h.getSkinIdentifier()).orElseThrow()),
                                List.of()
                        ))
                        .toList();
            }
        });
    }
}
