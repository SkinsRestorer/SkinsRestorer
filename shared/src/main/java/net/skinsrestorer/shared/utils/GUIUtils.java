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
package net.skinsrestorer.shared.utils;

import net.skinsrestorer.shared.gui.GUISkinEntry;
import net.skinsrestorer.shared.gui.PageInfo;
import net.skinsrestorer.shared.gui.SharedGUI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class GUIUtils {
    public static PageInfo getGUIPage(int page, GUIDataSource... sources) {
        if (page < 0) {
            page = 0;
        }

        List<GUIDataSource> enabledSources = Arrays.stream(sources)
                .filter(GUIDataSource::isEnabled)
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

            List<GUISkinEntry> sourceSkins = source.getGUISkins(sourceOffset, sourceLimit);
            skinPage.addAll(sourceSkins);

            if (sourceSkins.size() < sourceTotal - sourceOffset) {
                hasNextPage = true;
                break;
            }
        }

        return new PageInfo(
                page,
                page > 0,
                page < Integer.MAX_VALUE && hasNextPage,
                skinPage
        );
    }

    public interface GUIDataSource {
        boolean isEnabled();

        int getIndex();

        int getTotalSkins();

        List<GUISkinEntry> getGUISkins(int offset, int limit);
    }
}
