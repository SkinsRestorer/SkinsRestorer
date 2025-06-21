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

import javax.annotation.Nullable;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

public record SkinDefinition(Map<SkinSection, BufferedImage> sections,
                             SkinVariant variant,
                             @Nullable BufferedImage background) {
    public static SkinDefinition extractFrom(BufferedImage skinImage, @Nullable SkinVariant skinVariant, boolean preserveCustomPixels) {
        boolean isLegacyFormat = skinImage.getWidth() == 64 && skinImage.getHeight() == 32;
        boolean isModernFormat = skinImage.getWidth() == 64 && skinImage.getHeight() == 64;
        if (!isLegacyFormat && !isModernFormat) {
            throw new IllegalArgumentException("Skin image must be 64x64 pixels!");
        }

        // Scale up image to newer format if it's in legacy format
        if (isLegacyFormat) {
            BufferedImage resizedImage = new BufferedImage(
                    64,
                    64,
                    BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D g = resizedImage.createGraphics();
            g.drawImage(skinImage, 0, 0, null);
            g.dispose();

            skinImage = resizedImage;
            skinVariant = SkinVariant.CLASSIC; // Default to classic variant for legacy skins
        }

        if (skinVariant == null) {
            skinVariant = SkinVariant.detectVariant(skinImage);
        }

        BufferedImage backgroundImage = null;
        if (preserveCustomPixels) {
            backgroundImage = new BufferedImage(
                    skinImage.getWidth(),
                    skinImage.getHeight(),
                    BufferedImage.TYPE_INT_ARGB
            );
            var g = backgroundImage.createGraphics();
            g.drawImage(skinImage, 0, 0, null);
            g.dispose();

            for (var section : SkinSection.VALUES) {
                RectangleSection rectangleSection = section.getSectionVariants().get(skinVariant);
                // Clear the section area in the background image
                Graphics2D g2d = backgroundImage.createGraphics();
                g2d.setComposite(AlphaComposite.Clear);
                g2d.fillRect(
                        rectangleSection.x(),
                        rectangleSection.y(),
                        rectangleSection.width(),
                        rectangleSection.height()
                );
                g2d.dispose();
            }
        }

        var map = new EnumMap<SkinSection, BufferedImage>(SkinSection.class);
        for (SkinSection section : SkinSection.VALUES) {
            RectangleSection rectangleSection = section.getSectionVariants().get(skinVariant);
            BufferedImage subImage = skinImage.getSubimage(
                    rectangleSection.x(),
                    rectangleSection.y(),
                    rectangleSection.width(),
                    rectangleSection.height()
            );
            map.put(section, subImage);
        }

        if (isLegacyFormat) {
            for (SkinSection section : SkinSection.getTaggedSections(SkinTag.LEFT_LEG)) {
                map.put(section, map.get(SkinSection.valueOf(section.name().replaceFirst("LEFT_", "RIGHT_"))));
            }

            for (SkinSection section : SkinSection.getTaggedSections(SkinTag.LEFT_ARM)) {
                map.put(section, map.get(SkinSection.valueOf(section.name().replaceFirst("LEFT_", "RIGHT_"))));
            }
        }

        return new SkinDefinition(map, skinVariant, backgroundImage);
    }

    public BufferedImage export() {
        BufferedImage skinImage = new BufferedImage(
                64,
                64,
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g = skinImage.createGraphics();

        if (background != null) {
            g.drawImage(background, 0, 0, null);
        }

        for (var entry : sections.entrySet()) {
            SkinSection section = entry.getKey();
            BufferedImage sectionImage = entry.getValue();
            RectangleSection rectangleSection = section.getSectionVariants().get(variant);
            g.drawImage(sectionImage, rectangleSection.x(), rectangleSection.y(), null);
        }

        g.dispose();

        return skinImage;
    }
}
