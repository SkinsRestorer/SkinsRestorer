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
package net.skinsrestorer.shared.connections.mineskin.requests;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.connections.mineskin.MineSkinVariant;
import net.skinsrestorer.shared.connections.mineskin.MineSkinVisibility;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@RequiredArgsConstructor
@SuppressWarnings("unused")
@SuppressFBWarnings({"UWF_UNWRITTEN_FIELD", "URF_UNREAD_FIELD"})
public class MineSkinUrlRequest {
    private final @Nullable MineSkinVariant variant;
    private final @Nullable String name;
    private final @Nullable MineSkinVisibility visibility;
    private final @Nullable UUID cape;
    private final @Nullable String url;
}
