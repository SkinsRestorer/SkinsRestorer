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
package net.skinsrestorer.scissors.cape;

import lombok.Getter;
import lombok.ToString;
import net.skinsrestorer.scissors.RectangleSection;

import java.util.Set;

@Getter
@ToString
public enum CapeSection {
    // Cape
    CAPE_TOP(1, 0, 10, 1, CapeTag.CAPE, CapeTag.TOP),
    CAPE_BOTTOM(11, 0, 10, 1, CapeTag.CAPE, CapeTag.BOTTOM),
    CAPE_LEFT(0, 1, 1, 16, CapeTag.CAPE, CapeTag.LEFT),
    CAPE_FRONT(1, 1, 10, 16, CapeTag.CAPE, CapeTag.FRONT),
    CAPE_RIGHT(11, 1, 1, 16, CapeTag.CAPE, CapeTag.RIGHT),
    CAPE_BACK(12, 1, 10, 16, CapeTag.CAPE, CapeTag.BACK),
    // Elytra
    ELYTRA_TOP(24, 0, 8, 2, CapeTag.ELYTRA, CapeTag.TOP),
    ELYTRA_BOTTOM(32, 0, 8, 2, CapeTag.ELYTRA, CapeTag.BOTTOM),
    ELYTRA_LEFT(22, 2, 2, 20, CapeTag.ELYTRA, CapeTag.LEFT),
    // Back and front are swapped in elytra compared to cape
    ELYTRA_BACK(24, 2, 8, 20, CapeTag.ELYTRA, CapeTag.BACK),
    ELYTRA_RIGHT(32, 2, 2, 20, CapeTag.ELYTRA, CapeTag.RIGHT),
    // Back and front are swapped in elytra compared to cape
    ELYTRA_FRONT(34, 2, 8, 20, CapeTag.ELYTRA, CapeTag.FRONT);

    private final RectangleSection rectangleSection;
    private final Set<CapeTag> tags;

    CapeSection(int x, int y, int width, int height, CapeTag... tags) {
        this.rectangleSection = new RectangleSection(x, y, width, height);
        this.tags = Set.of(tags);
    }
}
