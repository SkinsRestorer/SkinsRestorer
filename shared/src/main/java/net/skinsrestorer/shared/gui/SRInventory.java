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

import net.skinsrestorer.shared.utils.ComponentString;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record SRInventory(int rows, ComponentString title, Map<Integer, Item> items) {
    public record Item(
            MaterialType materialType,
            ComponentString displayName,
            List<ComponentString> lore,
            @Nullable String textureHash,
            ClickEventHandler clickEventHandler
    ) {
    }

    public enum MaterialType {
        SKULL,
        WHITE_PANE,
        YELLOW_PANE,
        RED_PANE,
        GREEN_PANE,
    }
}
