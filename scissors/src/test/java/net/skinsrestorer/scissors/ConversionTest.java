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

import lombok.SneakyThrows;
import net.skinsrestorer.scissors.skin.SkinDefinition;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class ConversionTest {
    @Test
    @SneakyThrows
    public void loadAndSave() {
        System.setProperty("sun.java2d.uiScale", "2.0");
        var image = loadImage("/skin/ears.png");
        var skinDefinition = SkinDefinition.extractFrom(image, null, false);
        var extracted = skinDefinition.export();

        displayImage(extracted);
        while (!Thread.currentThread().isInterrupted()) {}
    }

    public BufferedImage loadImage(String path) {
        try (var stream = ConversionTest.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Resource not found: " + path);
            }
            return ImageIO.read(stream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load image from path: " + path, e);
        }
    }

    private void displayImage(BufferedImage image) {
        JFrame frame = new JFrame("BufferedImage Render");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ImagePanel(image));
        frame.setSize(220, 240);
        frame.setVisible(true);
    }

    public static class ImagePanel extends JPanel {
        private final BufferedImage image;

        public ImagePanel(BufferedImage image) {
            this.image = image;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(image, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
