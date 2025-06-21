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

import net.skinsrestorer.scissors.RectangleSection;

import java.awt.image.BufferedImage;

public enum SkinVariant {
    CLASSIC,
    SLIM;

    // While layer 2 is also trimmed, the overlay can still be transparent in classic, so it's not viable for detection.
    public static final RectangleSection SLIM_TRANSPARENT_RIGHT_ARM_SECTION = new RectangleSection(54, 20, 2, 12);
    public static final RectangleSection SLIM_TRANSPARENT_LEFT_ARM_SECTION = new RectangleSection(46, 52, 2, 12);

    public static final SkinVariant[] VALUES = values();

    public static SkinVariant detectVariant(BufferedImage image) {
        return isSlim(image) ? SLIM : CLASSIC;
    }

    public static boolean isSlim(BufferedImage image) {
        return anyTransparentPixel(image, SLIM_TRANSPARENT_RIGHT_ARM_SECTION) ||
                anyTransparentPixel(image, SLIM_TRANSPARENT_LEFT_ARM_SECTION) ;
    }

    private static boolean anyTransparentPixel(BufferedImage image, RectangleSection section) {
        return section.coordinateStream()
                .anyMatch(pixel -> {
                    int rgb = image.getRGB(pixel.x(), pixel.y());
                    return (rgb & 0xFF000000) == 0x00000000; // Check if pixel is transparent
                });
    }
}
