/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SkinHashingTest {
    @BeforeAll
    static void setupHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void fingerprintShouldBeStableForTheSameSkin() {
        BufferedImage skin = loadImage("/skin/classic.png");

        SkinHashing.SkinFingerprint first = SkinHashing.fingerprint(skin);
        SkinHashing.SkinFingerprint second = SkinHashing.fingerprint(skin);

        assertEquals(first, second, "Repeated hashing should produce the same fingerprint");
        assertEquals(SkinVariant.CLASSIC, first.variant(), "The classic test skin should resolve to the classic variant");
    }

    @Test
    void exactHashShouldChangeWhenPixelsChange() {
        BufferedImage original = loadImage("/skin/slim.png");
        BufferedImage modified = copyImage(original);
        modified.setRGB(10, 10, modified.getRGB(10, 10) ^ 0x00010101);

        assertNotEquals(
                SkinHashing.sha256(original),
                SkinHashing.sha256(modified),
                "A single pixel change should alter the exact normalized hash"
        );
    }

    @Test
    void perceptualHashShouldStayCloseForTinyEdits() {
        BufferedImage original = loadImage("/skin/classic.png");
        BufferedImage modified = copyImage(original);
        modified.setRGB(30, 20, modified.getRGB(30, 20) ^ 0x00010101);

        int distance = SkinHashing.hammingDistance(
                SkinHashing.perceptualHash(original),
                SkinHashing.perceptualHash(modified)
        );

        assertTrue(distance <= 4, "A tiny edit should stay within a very small perceptual distance");
    }

    @Test
    void perceptualHashShouldDetectLargeVisualDifferences() {
        BufferedImage original = loadImage("/skin/classic.png");
        BufferedImage different = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < different.getHeight(); y++) {
            for (int x = 0; x < different.getWidth(); x++) {
                different.setRGB(x, y, 0xFF00FFFF);
            }
        }

        int distance = SkinHashing.hammingDistance(
                SkinHashing.perceptualHash(original),
                SkinHashing.perceptualHash(different)
        );

        assertTrue(distance >= 32, "A very different image should not look similar to the perceptual hash");
    }

    @Test
    void invalidSkinSizeShouldBeRejected() {
        BufferedImage invalid = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);

        assertThrows(IllegalArgumentException.class, () -> SkinHashing.fingerprint(invalid));
    }

    private static BufferedImage loadImage(String path) {
        try (var stream = SkinHashingTest.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Resource not found: " + path);
            }
            return ImageIO.read(stream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load image from path: " + path, e);
        }
    }

    private static BufferedImage copyImage(BufferedImage image) {
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        copy.setData(image.getData());
        return copy;
    }
}
