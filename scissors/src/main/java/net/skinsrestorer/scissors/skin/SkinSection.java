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
import net.skinsrestorer.scissors.MapHelpers;
import net.skinsrestorer.scissors.RectangleSection;

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
    HEAD_BASE_RIGHT(16, 8, 8, 8, SkinTag.HEAD_BASE, SkinTag.RIGHT),
    HEAD_BASE_BACK(24, 8, 8, 8, SkinTag.HEAD_BASE, SkinTag.BACK),
    // Ears
    EARS_TOP(25, 0, 6, 1, SkinTag.EARS, SkinTag.TOP),
    EARS_BOTTOM(31, 0, 6, 1, SkinTag.EARS, SkinTag.BOTTOM),
    EARS_LEFT(24, 1, 1, 6, SkinTag.EARS, SkinTag.LEFT),
    EARS_RIGHT(31, 1, 1, 6, SkinTag.EARS, SkinTag.RIGHT),
    EARS_FRONT(25, 1, 6, 6, SkinTag.EARS, SkinTag.FRONT),
    EARS_BACK(32, 1, 6, 6, SkinTag.EARS, SkinTag.BACK),
    // Head overlay
    HEAD_OVERLAY_TOP(40, 0, 8, 8, SkinTag.HEAD_OVERLAY, SkinTag.TOP),
    HEAD_OVERLAY_BOTTOM(48, 0, 8, 8, SkinTag.HEAD_OVERLAY, SkinTag.BOTTOM),
    HEAD_OVERLAY_LEFT(32, 8, 8, 8, SkinTag.HEAD_OVERLAY, SkinTag.LEFT),
    HEAD_OVERLAY_FRONT(40, 8, 8, 8, SkinTag.HEAD_OVERLAY, SkinTag.FRONT),
    HEAD_OVERLAY_RIGHT(48, 8, 8, 8, SkinTag.HEAD_OVERLAY, SkinTag.RIGHT),
    HEAD_OVERLAY_BACK(56, 8, 8, 8, SkinTag.HEAD_OVERLAY, SkinTag.BACK),
    // Right leg base
    RIGHT_LEG_BASE_TOP(4, 16, 4, 4, SkinTag.RIGHT_LEG_BASE, SkinTag.TOP),
    RIGHT_LEG_BASE_BOTTOM(8, 16, 4, 4, SkinTag.RIGHT_LEG_BASE, SkinTag.BOTTOM),
    RIGHT_LEG_BASE_LEFT(0, 20, 4, 12, SkinTag.RIGHT_LEG_BASE, SkinTag.LEFT),
    RIGHT_LEG_BASE_FRONT(4, 20, 4, 12, SkinTag.RIGHT_LEG_BASE, SkinTag.FRONT),
    RIGHT_LEG_BASE_RIGHT(8, 20, 4, 12, SkinTag.RIGHT_LEG_BASE, SkinTag.RIGHT),
    RIGHT_LEG_BASE_BACK(12, 20, 4, 12, SkinTag.RIGHT_LEG_BASE, SkinTag.BACK),
    // Right leg overlay
    RIGHT_LEG_OVERLAY_TOP(4, 32, 4, 4, SkinTag.RIGHT_LEG_OVERLAY, SkinTag.TOP),
    RIGHT_LEG_OVERLAY_BOTTOM(8, 32, 4, 4, SkinTag.RIGHT_LEG_OVERLAY, SkinTag.BOTTOM),
    RIGHT_LEG_OVERLAY_LEFT(0, 36, 4, 12, SkinTag.RIGHT_LEG_OVERLAY, SkinTag.LEFT),
    RIGHT_LEG_OVERLAY_FRONT(4, 36, 4, 12, SkinTag.RIGHT_LEG_OVERLAY, SkinTag.FRONT),
    RIGHT_LEG_OVERLAY_RIGHT(8, 36, 4, 12, SkinTag.RIGHT_LEG_OVERLAY, SkinTag.RIGHT),
    RIGHT_LEG_OVERLAY_BACK(12, 36, 4, 12, SkinTag.RIGHT_LEG_OVERLAY, SkinTag.BACK),
    // Left leg overlay
    LEFT_LEG_OVERLAY_TOP(4, 48, 4, 4, SkinTag.LEFT_LEG_OVERLAY, SkinTag.TOP),
    LEFT_LEG_OVERLAY_BOTTOM(8, 48, 4, 4, SkinTag.LEFT_LEG_OVERLAY, SkinTag.BOTTOM),
    LEFT_LEG_OVERLAY_LEFT(0, 52, 4, 12, SkinTag.LEFT_LEG_OVERLAY, SkinTag.LEFT),
    LEFT_LEG_OVERLAY_FRONT(4, 52, 4, 12, SkinTag.LEFT_LEG_OVERLAY, SkinTag.FRONT),
    LEFT_LEG_OVERLAY_RIGHT(8, 52, 4, 12, SkinTag.LEFT_LEG_OVERLAY, SkinTag.RIGHT),
    LEFT_LEG_OVERLAY_BACK(12, 52, 4, 12, SkinTag.LEFT_LEG_OVERLAY, SkinTag.BACK),
    // Left leg base
    LEFT_LEG_BASE_TOP(20, 48, 4, 4, SkinTag.LEFT_LEG_BASE, SkinTag.TOP),
    LEFT_LEG_BASE_BOTTOM(24, 48, 4, 4, SkinTag.LEFT_LEG_BASE, SkinTag.BOTTOM),
    LEFT_LEG_BASE_LEFT(16, 52, 4, 12, SkinTag.LEFT_LEG_BASE, SkinTag.LEFT),
    LEFT_LEG_BASE_FRONT(20, 52, 4, 12, SkinTag.LEFT_LEG_BASE, SkinTag.FRONT),
    LEFT_LEG_BASE_RIGHT(24, 52, 4, 12, SkinTag.LEFT_LEG_BASE, SkinTag.RIGHT),
    LEFT_LEG_BASE_BACK(28, 52, 4, 12, SkinTag.LEFT_LEG_BASE, SkinTag.BACK),
    // Left arm base
    LEFT_ARM_BASE_TOP(MapHelpers.mapped(SkinVariant.class, variant -> switch (variant) {
        case SkinVariant.CLASSIC -> new RectangleSection(36, 48, 4, 4);
        case SkinVariant.SLIM -> new RectangleSection(36, 48, 3, 4);
    }), SkinTag.LEFT_ARM_BASE, SkinTag.TOP),
    LEFT_ARM_BASE_BOTTOM(MapHelpers.mapped(SkinVariant.class, variant -> switch (variant) {
        case SkinVariant.CLASSIC -> new RectangleSection(40, 48, 4, 4);
        case SkinVariant.SLIM -> new RectangleSection(39, 48, 3, 4);
    }), SkinTag.LEFT_ARM_BASE, SkinTag.BOTTOM),
    LEFT_ARM_BASE_LEFT(32, 52, 4, 12, SkinTag.LEFT_ARM_BASE, SkinTag.LEFT),
    LEFT_ARM_BASE_FRONT(MapHelpers.mapped(SkinVariant.class, variant -> switch (variant) {
        case SkinVariant.CLASSIC -> new RectangleSection(36, 52, 4, 12);
        case SkinVariant.SLIM -> new RectangleSection(36, 52, 3, 12);
    }), SkinTag.LEFT_ARM_BASE, SkinTag.FRONT),
    LEFT_ARM_BASE_RIGHT(MapHelpers.mapped(SkinVariant.class, variant -> switch (variant) {
        case SkinVariant.CLASSIC -> new RectangleSection(40, 52, 4, 12);
        case SkinVariant.SLIM -> new RectangleSection(39, 52, 4, 12);
    }), SkinTag.LEFT_ARM_BASE, SkinTag.RIGHT),
    LEFT_ARM_BASE_BACK(MapHelpers.mapped(SkinVariant.class, variant -> switch (variant) {
        case SkinVariant.CLASSIC -> new RectangleSection(44, 52, 4, 12);
        case SkinVariant.SLIM -> new RectangleSection(43, 52, 3, 12);
    }), SkinTag.LEFT_ARM_BASE, SkinTag.BACK),
    // Left arm overlay
    LEFT_ARM_OVERLAY_TOP(MapHelpers.mapped(SkinVariant.class, variant -> switch (variant) {
        case SkinVariant.CLASSIC -> new RectangleSection(52, 48, 4, 4);
        case SkinVariant.SLIM -> new RectangleSection(52, 48, 3, 4);
    }), SkinTag.LEFT_ARM_OVERLAY, SkinTag.TOP),
    LEFT_ARM_OVERLAY_BOTTOM(MapHelpers.mapped(SkinVariant.class, variant -> switch (variant) {
        case SkinVariant.CLASSIC -> new RectangleSection(56, 48, 4, 4);
        case SkinVariant.SLIM -> new RectangleSection(55, 48, 3, 4);
    }), SkinTag.LEFT_ARM_OVERLAY, SkinTag.BOTTOM),
    LEFT_ARM_OVERLAY_LEFT(48, 52, 4, 12, SkinTag.LEFT_ARM_OVERLAY, SkinTag.LEFT),
    LEFT_ARM_OVERLAY_FRONT(MapHelpers.mapped(SkinVariant.class, variant -> switch (variant) {
        case SkinVariant.CLASSIC -> new RectangleSection(52, 52, 4, 12);
        case SkinVariant.SLIM -> new RectangleSection(52, 52, 3, 12);
    }), SkinTag.LEFT_ARM_OVERLAY, SkinTag.FRONT),
    LEFT_ARM_OVERLAY_RIGHT(MapHelpers.mapped(SkinVariant.class, variant -> switch (variant) {
        case SkinVariant.CLASSIC -> new RectangleSection(56, 52, 4, 12);
        case SkinVariant.SLIM -> new RectangleSection(55, 52, 4, 12);
    }), SkinTag.LEFT_ARM_OVERLAY, SkinTag.RIGHT),
    LEFT_ARM_OVERLAY_BACK(MapHelpers.mapped(SkinVariant.class, variant -> switch (variant) {
        case SkinVariant.CLASSIC -> new RectangleSection(60, 52, 4, 12);
        case SkinVariant.SLIM -> new RectangleSection(59, 52, 3, 12);
    }), SkinTag.LEFT_ARM_OVERLAY, SkinTag.BACK),
    // Torso base
    TORSO_BASE_TOP(20, 16, 8, 4, SkinTag.TORSO_BASE, SkinTag.TOP),
    TORSO_BASE_BOTTOM(28, 16, 8, 4, SkinTag.TORSO_BASE, SkinTag.BOTTOM),
    TORSO_BASE_LEFT(16, 20, 4, 12, SkinTag.TORSO_BASE, SkinTag.LEFT),
    TORSO_BASE_FRONT(20, 20, 8, 12, SkinTag.TORSO_BASE, SkinTag.FRONT),
    TORSO_BASE_RIGHT(28, 20, 4, 12, SkinTag.TORSO_BASE, SkinTag.RIGHT),
    TORSO_BASE_BACK(36, 20, 8, 12, SkinTag.TORSO_BASE, SkinTag.BACK),
    // Torso overlay
    TORSO_OVERLAY_TOP(20, 32, 8, 4, SkinTag.TORSO_OVERLAY, SkinTag.TOP),
    TORSO_OVERLAY_BOTTOM(28, 32, 8, 4, SkinTag.TORSO_OVERLAY, SkinTag.BOTTOM),
    TORSO_OVERLAY_LEFT(16, 36, 4, 12, SkinTag.TORSO_OVERLAY, SkinTag.LEFT),
    TORSO_OVERLAY_FRONT(20, 36, 8, 12, SkinTag.TORSO_OVERLAY, SkinTag.FRONT),
    TORSO_OVERLAY_RIGHT(28, 36, 4, 12, SkinTag.TORSO_OVERLAY, SkinTag.RIGHT),
    TORSO_OVERLAY_BACK(36, 36, 8, 12, SkinTag.TORSO_OVERLAY, SkinTag.BACK),
    // Right arm base
    RIGHT_ARM_BASE_TOP(MapHelpers.mapped(SkinVariant.class, variant -> switch (variant) {
        case SkinVariant.CLASSIC -> new RectangleSection(44, 16, 4, 4);
        case SkinVariant.SLIM -> new RectangleSection(44, 16, 3, 4);
    }), SkinTag.RIGHT_ARM_BASE, SkinTag.TOP),
    RIGHT_ARM_BASE_BOTTOM(MapHelpers.mapped(SkinVariant.class, variant -> switch (variant) {
        case SkinVariant.CLASSIC -> new RectangleSection(48, 16, 4, 4);
        case SkinVariant.SLIM -> new RectangleSection(47, 16, 3, 4);
    }), SkinTag.RIGHT_ARM_BASE, SkinTag.BOTTOM),
    RIGHT_ARM_BASE_LEFT(40, 20, 4, 12, SkinTag.RIGHT_ARM_BASE, SkinTag.LEFT),
    RIGHT_ARM_BASE_FRONT(MapHelpers.mapped(SkinVariant.class, variant -> switch (variant) {
        case SkinVariant.CLASSIC -> new RectangleSection(44, 20, 4, 12);
        case SkinVariant.SLIM -> new RectangleSection(44, 20, 3, 12);
    }), SkinTag.RIGHT_ARM_BASE, SkinTag.FRONT),
    RIGHT_ARM_BASE_RIGHT(MapHelpers.mapped(SkinVariant.class, variant -> switch (variant) {
        case SkinVariant.CLASSIC -> new RectangleSection(48, 20, 4, 12);
        case SkinVariant.SLIM -> new RectangleSection(47, 20, 4, 12);
    }), SkinTag.RIGHT_ARM_BASE, SkinTag.RIGHT),
    RIGHT_ARM_BASE_BACK(MapHelpers.mapped(SkinVariant.class, variant -> switch (variant) {
        case SkinVariant.CLASSIC -> new RectangleSection(52, 20, 4, 12);
        case SkinVariant.SLIM -> new RectangleSection(51, 20, 3, 12);
    }), SkinTag.RIGHT_ARM_BASE, SkinTag.BACK),
    // Right arm overlay
    RIGHT_ARM_OVERLAY_TOP(MapHelpers.mapped(SkinVariant.class, variant -> switch (variant) {
        case SkinVariant.CLASSIC -> new RectangleSection(44, 32, 4, 4);
        case SkinVariant.SLIM -> new RectangleSection(44, 32, 3, 4);
    }), SkinTag.RIGHT_ARM_OVERLAY, SkinTag.TOP),
    RIGHT_ARM_OVERLAY_BOTTOM(MapHelpers.mapped(SkinVariant.class, variant -> switch (variant) {
        case SkinVariant.CLASSIC -> new RectangleSection(48, 32, 4, 4);
        case SkinVariant.SLIM -> new RectangleSection(47, 32, 3, 4);
    }), SkinTag.RIGHT_ARM_OVERLAY, SkinTag.BOTTOM),
    RIGHT_ARM_OVERLAY_LEFT(40, 36, 4, 12, SkinTag.RIGHT_ARM_OVERLAY, SkinTag.LEFT),
    RIGHT_ARM_OVERLAY_FRONT(MapHelpers.mapped(SkinVariant.class, variant -> switch (variant) {
        case SkinVariant.CLASSIC -> new RectangleSection(44, 36, 4, 12);
        case SkinVariant.SLIM -> new RectangleSection(44, 36, 3, 12);
    }), SkinTag.RIGHT_ARM_OVERLAY, SkinTag.FRONT),
    RIGHT_ARM_OVERLAY_RIGHT(MapHelpers.mapped(SkinVariant.class, variant -> switch (variant) {
        case SkinVariant.CLASSIC -> new RectangleSection(48, 36, 4, 12);
        case SkinVariant.SLIM -> new RectangleSection(47, 36, 4, 12);
    }), SkinTag.RIGHT_ARM_OVERLAY, SkinTag.RIGHT),
    RIGHT_ARM_OVERLAY_BACK(MapHelpers.mapped(SkinVariant.class, variant -> switch (variant) {
        case SkinVariant.CLASSIC -> new RectangleSection(52, 36, 4, 12);
        case SkinVariant.SLIM -> new RectangleSection(51, 36, 3, 12);
    }), SkinTag.RIGHT_ARM_OVERLAY, SkinTag.BACK);

    public static final SkinSection[] VALUES = SkinSection.values();

    private final Map<SkinVariant, RectangleSection> sectionVariants;
    private final Set<SkinTag> directTags;
    private final Set<SkinTag> inheritedTags;

    SkinSection(int x, int y, int width, int height, SkinTag... tags) {
        this(MapHelpers.allSetTo(SkinVariant.class, new RectangleSection(x, y, width, height)), tags);
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
