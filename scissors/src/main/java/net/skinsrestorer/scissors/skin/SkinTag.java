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

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@ToString
public enum SkinTag {
    // Body parts
    // Head
    HEAD_BASE,
    HEAD_OVERLAY,
    HEAD(HEAD_BASE, HEAD_OVERLAY),
    // Ears
    EARS,
    // Right leg
    RIGHT_LEG_BASE,
    RIGHT_LEG_OVERLAY,
    RIGHT_LEG(RIGHT_LEG_BASE, RIGHT_LEG_OVERLAY),
    // Left leg
    LEFT_LEG_BASE,
    LEFT_LEG_OVERLAY,
    LEFT_LEG(LEFT_LEG_BASE, LEFT_LEG_OVERLAY),
    // Legs
    LEGS(RIGHT_LEG, LEFT_LEG),
    // Left arm
    LEFT_ARM_BASE,
    LEFT_ARM_OVERLAY,
    LEFT_ARM(LEFT_ARM_BASE, LEFT_ARM_OVERLAY),
    // Right arm
    RIGHT_ARM_BASE,
    RIGHT_ARM_OVERLAY,
    RIGHT_ARM(RIGHT_ARM_BASE, RIGHT_ARM_OVERLAY),
    // Arms
    ARMS(LEFT_ARM, RIGHT_ARM),
    // Torso
    TORSO_BASE,
    TORSO_OVERLAY,
    TORSO(TORSO_BASE, TORSO_OVERLAY),
    // Layers
    BASE(HEAD_BASE, RIGHT_LEG_BASE, LEFT_LEG_BASE, LEFT_ARM_BASE, RIGHT_ARM_BASE, TORSO_BASE),
    OVERLAY(HEAD_OVERLAY, RIGHT_LEG_OVERLAY, LEFT_LEG_OVERLAY, LEFT_ARM_OVERLAY, RIGHT_ARM_OVERLAY, TORSO_OVERLAY),
    // Body sides
    FRONT,
    BACK,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
    // Full skin
    FULL(HEAD, LEGS, ARMS, TORSO, EARS);

    public static final SkinTag[] VALUES = values();

    private final Set<SkinTag> parents;
    @ToString.Exclude
    private final Set<SkinTag> inheritedTags;

    SkinTag(SkinTag... parents) {
        this.parents = Set.of(parents);
        this.inheritedTags = Stream.concat(
                        Stream.of(this),
                        this.parents.stream().flatMap(tag -> tag.getInheritedTags().stream()))
                .collect(Collectors.toUnmodifiableSet());
    }
}
