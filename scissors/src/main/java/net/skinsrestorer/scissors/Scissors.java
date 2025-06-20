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
package net.skinsrestorer.scissors;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Scissors {
    private Scissors() {
    }

    public static BufferedImage applyOnEachPixel(BufferedImage image, ARGBPixelProcessor processor) {
        BufferedImage processedImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int processedRgb = processor.applyAsInt(rgb);
                processedImage.setRGB(x, y, processedRgb);
            }
        }

        return processedImage;
    }

    public static BufferedImage flipImage(BufferedImage image, boolean horizontal) {
        BufferedImage flippedImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int newX = horizontal ? image.getWidth() - 1 - x : x;
                int newY = horizontal ? y : image.getHeight() - 1 - y;
                flippedImage.setRGB(newX, newY, image.getRGB(x, y));
            }
        }

        return flippedImage;
    }

    public static BufferedImage rotateImage(BufferedImage image, double angle) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage rotatedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = rotatedImage.createGraphics();
        g2d.rotate(Math.toRadians(angle), width / 2.0, height / 2.0);
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        return rotatedImage;
    }

    // Zoom image content without changing viewport size
    public static BufferedImage zoomImage(BufferedImage image, double scaleX, double scaleY) {
        BufferedImage zoomedImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = zoomedImage.createGraphics();
        g2d.scale(scaleX, scaleY);
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        return zoomedImage;
    }
}
