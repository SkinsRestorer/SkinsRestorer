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
package net.skinsrestorer.scissors.skin;

import lombok.Getter;
import lombok.ToString;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@ToString
public enum SkinSection {
    // Head base
    HEAD_BASE_TOP(8, 0, 8, 8, SkinTag.HEAD_BASE, SkinTag.TOP),
    HEAD_BASE_BOTTOM(16, 0, 8, 8, SkinTag.HEAD_BASE, SkinTag.BOTTOM),
    HEAD_BASE_LEFT(0, 8, 8, 8, SkinTag.HEAD_BASE, SkinTag.LEFT),
    HEAD_BASE_FRONT(8, 8, 8, 8, SkinTag.HEAD_BASE, SkinTag.FRONT),
    HEAD_BASE_BACK(16, 8, 8, 8, SkinTag.HEAD_BASE, SkinTag.BACK),
    HEAD_BASE_RIGHT(24, 8, 8, 8, SkinTag.HEAD_BASE, SkinTag.RIGHT),
    // Ears
    // TODO: Implement ear sections
    // Head overlay
    HEAD_OVERLAY_TOP(40, 0, 8, 8, SkinTag.HEAD_OVERLAY, SkinTag.TOP),
    HEAD_OVERLAY_BOTTOM(48, 0, 8, 8, SkinTag.HEAD_OVERLAY, SkinTag.BOTTOM),
    HEAD_OVERLAY_LEFT(32, 8, 8, 8, SkinTag.HEAD_OVERLAY, SkinTag.LEFT),
    HEAD_OVERLAY_FRONT(40, 8, 8, 8, SkinTag.HEAD_OVERLAY, SkinTag.FRONT),
    HEAD_OVERLAY_BACK(48, 8, 8, 8, SkinTag.HEAD_OVERLAY, SkinTag.BACK),
    HEAD_OVERLAY_RIGHT(56, 8, 8, 8, SkinTag.HEAD_OVERLAY, SkinTag.RIGHT),
    // Right leg base
    RIGHT_LEG_BASE_TOP(4, 16, 4, 4, SkinTag.RIGHT_LEG_BASE, SkinTag.TOP),
    RIGHT_LEG_BASE_BOTTOM(8, 16, 4, 4, SkinTag.RIGHT_LEG_BASE, SkinTag.BOTTOM),
    RIGHT_LEG_BASE_LEFT(0, 20, 4, 12, SkinTag.RIGHT_LEG_BASE, SkinTag.LEFT),
    RIGHT_LEG_BASE_FRONT(4, 20, 4, 12, SkinTag.RIGHT_LEG_BASE, SkinTag.FRONT),
    RIGHT_LEG_BASE_BACK(8, 20, 4, 12, SkinTag.RIGHT_LEG_BASE, SkinTag.BACK),
    RIGHT_LEG_BASE_RIGHT(12, 20, 4, 12, SkinTag.RIGHT_LEG_BASE, SkinTag.RIGHT),
    // Right leg overlay
    RIGHT_LEG_OVERLAY_TOP(4, 32, 4, 4, SkinTag.RIGHT_LEG_OVERLAY, SkinTag.TOP),
    RIGHT_LEG_OVERLAY_BOTTOM(8, 32, 4, 4, SkinTag.RIGHT_LEG_OVERLAY, SkinTag.BOTTOM),
    RIGHT_LEG_OVERLAY_LEFT(0, 36, 4, 12, SkinTag.RIGHT_LEG_OVERLAY, SkinTag.LEFT),
    RIGHT_LEG_OVERLAY_FRONT(4, 36, 4, 12, SkinTag.RIGHT_LEG_OVERLAY, SkinTag.FRONT),
    RIGHT_LEG_OVERLAY_BACK(8, 36, 4, 12, SkinTag.RIGHT_LEG_OVERLAY, SkinTag.BACK),
    RIGHT_LEG_OVERLAY_RIGHT(12, 36, 4, 12, SkinTag.RIGHT_LEG_OVERLAY, SkinTag.RIGHT),
    // Left leg overlay
    LEFT_LEG_OVERLAY_TOP(4, 48, 4, 4, SkinTag.LEFT_LEG_OVERLAY, SkinTag.TOP),
    LEFT_LEG_OVERLAY_BOTTOM(8, 48, 4, 4, SkinTag.LEFT_LEG_OVERLAY, SkinTag.BOTTOM),
    LEFT_LEG_OVERLAY_LEFT(0, 52, 4, 12, SkinTag.LEFT_LEG_OVERLAY, SkinTag.LEFT),
    LEFT_LEG_OVERLAY_FRONT(4, 52, 4, 12, SkinTag.LEFT_LEG_OVERLAY, SkinTag.FRONT),
    LEFT_LEG_OVERLAY_BACK(8, 52, 4, 12, SkinTag.LEFT_LEG_OVERLAY, SkinTag.BACK),
    LEFT_LEG_OVERLAY_RIGHT(12, 52, 4, 12, SkinTag.LEFT_LEG_OVERLAY, SkinTag.RIGHT),
    // Left leg base
    LEFT_LEG_BASE_TOP(20, 48, 4, 4, SkinTag.LEFT_LEG_BASE, SkinTag.TOP),
    LEFT_LEG_BASE_BOTTOM(24, 48, 4, 4, SkinTag.LEFT_LEG_BASE, SkinTag.BOTTOM),
    LEFT_LEG_BASE_LEFT(16, 52, 4, 12, SkinTag.LEFT_LEG_BASE, SkinTag.LEFT),
    LEFT_LEG_BASE_FRONT(20, 52, 4, 12, SkinTag.LEFT_LEG_BASE, SkinTag.FRONT),
    LEFT_LEG_BASE_BACK(24, 52, 4, 12, SkinTag.LEFT_LEG_BASE, SkinTag.BACK),
    LEFT_LEG_BASE_RIGHT(28, 52, 4, 12, SkinTag.LEFT_LEG_BASE, SkinTag.RIGHT),
    // Left arm base
    LEFT_ARM_BASE_TOP(Map.of(
            SkinVariant.CLASSIC, new RectangleSection(36, 48, 4, 4),
            SkinVariant.SLIM, new RectangleSection(36, 48, 3, 4)
    ), SkinTag.LEFT_ARM_BASE, SkinTag.TOP),
    LEFT_ARM_BASE_BOTTOM(Map.of(
            SkinVariant.CLASSIC, new RectangleSection(40, 48, 4, 4),
            SkinVariant.SLIM, new RectangleSection(39, 48, 3, 4)
    ), SkinTag.LEFT_ARM_BASE, SkinTag.BOTTOM),
    LEFT_ARM_BASE_LEFT(32, 52, 4, 12, SkinTag.LEFT_ARM_BASE, SkinTag.LEFT),
    LEFT_ARM_BASE_FRONT(Map.of(
            SkinVariant.CLASSIC, new RectangleSection(36, 52, 4, 12),
            SkinVariant.SLIM, new RectangleSection(36, 52, 3, 12)
    ), SkinTag.LEFT_ARM_BASE, SkinTag.FRONT),
    LEFT_ARM_BASE_BACK(Map.of(
            SkinVariant.CLASSIC, new RectangleSection(40, 52, 4, 12),
            SkinVariant.SLIM, new RectangleSection(39, 52, 4, 12)
    ), SkinTag.LEFT_ARM_BASE, SkinTag.BACK),
    LEFT_ARM_BASE_RIGHT(Map.of(
            SkinVariant.CLASSIC, new RectangleSection(44, 52, 4, 12),
            SkinVariant.SLIM, new RectangleSection(43, 52, 3, 12)
    ), SkinTag.LEFT_ARM_BASE, SkinTag.RIGHT),
    // Left arm overlay
    LEFT_ARM_OVERLAY_TOP(52, 48, 4, 4, SkinTag.LEFT_ARM_OVERLAY, SkinTag.TOP),
    LEFT_ARM_OVERLAY_BOTTOM(56, 48, 4, 4, SkinTag.LEFT_ARM_OVERLAY, SkinTag.BOTTOM),
    LEFT_ARM_OVERLAY_LEFT(48, 52, 4, 12, SkinTag.LEFT_ARM_OVERLAY, SkinTag.LEFT),
    LEFT_ARM_OVERLAY_FRONT(52, 52, 4, 12, SkinTag.LEFT_ARM_OVERLAY, SkinTag.FRONT),
    LEFT_ARM_OVERLAY_BACK(56, 52, 4, 12, SkinTag.LEFT_ARM_OVERLAY, SkinTag.BACK),
    LEFT_ARM_OVERLAY_RIGHT(60, 52, 4, 12, SkinTag.LEFT_ARM_OVERLAY, SkinTag.RIGHT),
    // Torso base
    TORSO_BASE_TOP(20, 16, 8, 4, SkinTag.TORSO_BASE, SkinTag.TOP),
    TORSO_BASE_BOTTOM(28, 16, 8, 4, SkinTag.TORSO_BASE, SkinTag.BOTTOM),
    TORSO_BASE_LEFT(16, 20, 4, 12, SkinTag.TORSO_BASE, SkinTag.LEFT),
    TORSO_BASE_FRONT(20, 20, 8, 12, SkinTag.TORSO_BASE, SkinTag.FRONT),
    TORSO_BASE_BACK(28, 20, 8, 12, SkinTag.TORSO_BASE, SkinTag.BACK),
    TORSO_BASE_RIGHT(36, 20, 4, 12, SkinTag.TORSO_BASE, SkinTag.RIGHT),
    // Torso overlay
    TORSO_OVERLAY_TOP(20, 32, 8, 4, SkinTag.TORSO_OVERLAY, SkinTag.TOP),
    TORSO_OVERLAY_BOTTOM(28, 32, 8, 4, SkinTag.TORSO_OVERLAY, SkinTag.BOTTOM),
    TORSO_OVERLAY_LEFT(16, 36, 4, 12, SkinTag.TORSO_OVERLAY, SkinTag.LEFT),
    TORSO_OVERLAY_FRONT(20, 36, 8, 12, SkinTag.TORSO_OVERLAY, SkinTag.FRONT),
    TORSO_OVERLAY_BACK(28, 36, 8, 12, SkinTag.TORSO_OVERLAY, SkinTag.BACK),
    TORSO_OVERLAY_RIGHT(36, 36, 4, 12, SkinTag.TORSO_OVERLAY, SkinTag.RIGHT),
    // Right arm base
    RIGHT_ARM_BASE_TOP(44, 16, 4, 4, SkinTag.RIGHT_ARM_BASE, SkinTag.TOP),
    RIGHT_ARM_BASE_BOTTOM(48, 16, 4, 4, SkinTag.RIGHT_ARM_BASE, SkinTag.BOTTOM),
    RIGHT_ARM_BASE_LEFT(40, 20, 4, 12, SkinTag.RIGHT_ARM_BASE, SkinTag.LEFT),
    RIGHT_ARM_BASE_FRONT(44, 20, 4, 12, SkinTag.RIGHT_ARM_BASE, SkinTag.FRONT),
    RIGHT_ARM_BASE_BACK(48, 20, 4, 12, SkinTag.RIGHT_ARM_BASE, SkinTag.BACK),
    RIGHT_ARM_BASE_RIGHT(52, 20, 4, 12, SkinTag.RIGHT_ARM_BASE, SkinTag.RIGHT),
    // Right arm overlay
    RIGHT_ARM_OVERLAY_TOP(44, 32, 4, 4, SkinTag.RIGHT_ARM_OVERLAY, SkinTag.TOP),
    RIGHT_ARM_OVERLAY_BOTTOM(48, 32, 4, 4, SkinTag.RIGHT_ARM_OVERLAY, SkinTag.BOTTOM),
    RIGHT_ARM_OVERLAY_LEFT(40, 36, 4, 12, SkinTag.RIGHT_ARM_OVERLAY, SkinTag.LEFT),
    RIGHT_ARM_OVERLAY_FRONT(44, 36, 4, 12, SkinTag.RIGHT_ARM_OVERLAY, SkinTag.FRONT),
    RIGHT_ARM_OVERLAY_BACK(48, 36, 4, 12, SkinTag.RIGHT_ARM_OVERLAY, SkinTag.BACK),
    RIGHT_ARM_OVERLAY_RIGHT(52, 36, 4, 12, SkinTag.RIGHT_ARM_OVERLAY, SkinTag.RIGHT);

    public static final SkinSection[] VALUES = SkinSection.values();

    private final Map<SkinVariant, RectangleSection> sectionVariants;
    private final Set<SkinTag> directTags;
    private final Set<SkinTag> inheritedTags;

    SkinSection(int x, int y, int width, int height, SkinTag... tags) {
        this(Arrays.stream(SkinVariant.VALUES).collect(() -> new EnumMap<>(SkinVariant.class),
                (m, e) -> m.put(e, new RectangleSection(x, y, width, height)),
                Map::putAll), tags);
    }

    SkinSection(Map<SkinVariant, RectangleSection> sectionVariants, SkinTag... tags) {
        this.sectionVariants = sectionVariants;
        this.directTags = Set.of(tags);
        this.inheritedTags = Arrays.stream(SkinTag.VALUES)
                .filter(tag -> tag.getInheritedTags().stream().anyMatch(directTags::contains))
                .collect(Collectors.toUnmodifiableSet());
    }

    public static List<SkinSection> getTaggedSections(SkinTag tag) {
        return Arrays.stream(VALUES)
                .filter(section -> section.getInheritedTags().contains(tag))
                .toList();
    }
}
