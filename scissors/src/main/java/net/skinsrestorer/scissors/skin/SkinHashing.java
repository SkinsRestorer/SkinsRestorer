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

import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility methods for generating stable hashes for Minecraft skin images.
 * <p>
 * The exact hash intentionally works on a normalized pixel representation rather than on the original PNG bytes.
 * This makes matches resilient to harmless file differences such as PNG compression level, ancillary metadata,
 * or whether the source was originally 64x32 or 64x64.
 */
public final class SkinHashing {
    private static final HexFormat HEX = HexFormat.of();
    private static final int PERCEPTUAL_HASH_WIDTH = 17;
    private static final int PERCEPTUAL_HASH_HEIGHT = 16;

    private SkinHashing() {
    }

    public static SkinFingerprint fingerprint(BufferedImage skinImage) {
        return fingerprint(skinImage, null);
    }

    public static SkinFingerprint fingerprint(BufferedImage skinImage, @Nullable SkinVariant variantHint) {
        NormalizedSkin normalizedSkin = normalizeInternal(skinImage, variantHint);
        BufferedImage normalizedImage = normalizedSkin.image();

        return new SkinFingerprint(
                sha256OfNormalizedImage(normalizedImage, normalizedSkin.variant()),
                perceptualHashOfNormalizedImage(normalizedImage),
                normalizedSkin.variant()
        );
    }

    public static BufferedImage normalize(BufferedImage skinImage) {
        return normalize(skinImage, null);
    }

    public static BufferedImage normalize(BufferedImage skinImage, @Nullable SkinVariant variantHint) {
        return normalizeInternal(skinImage, variantHint).image();
    }

    public static String sha256(BufferedImage skinImage) {
        return sha256(skinImage, null);
    }

    public static String sha256(BufferedImage skinImage, @Nullable SkinVariant variantHint) {
        return fingerprint(skinImage, variantHint).sha256();
    }

    public static String perceptualHash(BufferedImage skinImage) {
        return perceptualHash(skinImage, null);
    }

    public static String perceptualHash(BufferedImage skinImage, @Nullable SkinVariant variantHint) {
        return fingerprint(skinImage, variantHint).perceptualHash();
    }

    public static int hammingDistance(String leftHash, String rightHash) {
        if (leftHash.length() != rightHash.length()) {
            throw new IllegalArgumentException("Hashes must have the same length");
        }

        int distance = 0;
        for (int i = 0; i < leftHash.length(); i++) {
            distance += Integer.bitCount(hexToNibble(leftHash.charAt(i)) ^ hexToNibble(rightHash.charAt(i)));
        }

        return distance;
    }

    private static NormalizedSkin normalizeInternal(BufferedImage skinImage, @Nullable SkinVariant variantHint) {
        SkinDefinition definition = SkinDefinition.extractFrom(skinImage, variantHint, true);
        return new NormalizedSkin(definition.export(), definition.variant());
    }

    private static String sha256OfNormalizedImage(BufferedImage normalizedImage, SkinVariant variant) {
        ByteBuffer buffer = ByteBuffer.allocate(9 + normalizedImage.getWidth() * normalizedImage.getHeight() * Integer.BYTES);
        buffer.putInt(normalizedImage.getWidth());
        buffer.putInt(normalizedImage.getHeight());
        buffer.put((byte) variant.ordinal());

        for (int y = 0; y < normalizedImage.getHeight(); y++) {
            for (int x = 0; x < normalizedImage.getWidth(); x++) {
                buffer.putInt(normalizedImage.getRGB(x, y));
            }
        }

        return HEX.formatHex(sha256Bytes(buffer.array()));
    }

    private static String perceptualHashOfNormalizedImage(BufferedImage normalizedImage) {
        BufferedImage grayscale = new BufferedImage(PERCEPTUAL_HASH_WIDTH, PERCEPTUAL_HASH_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = grayscale.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(normalizedImage, 0, 0, PERCEPTUAL_HASH_WIDTH, PERCEPTUAL_HASH_HEIGHT, null);
        graphics.dispose();

        Raster raster = grayscale.getRaster();
        byte[] hashBytes = new byte[(PERCEPTUAL_HASH_WIDTH - 1) * PERCEPTUAL_HASH_HEIGHT / Byte.SIZE];
        int bitIndex = 0;

        for (int y = 0; y < PERCEPTUAL_HASH_HEIGHT; y++) {
            for (int x = 0; x < PERCEPTUAL_HASH_WIDTH - 1; x++) {
                int left = raster.getSample(x, y, 0);
                int right = raster.getSample(x + 1, y, 0);
                if (left > right) {
                    hashBytes[bitIndex / Byte.SIZE] |= (byte) (1 << (7 - (bitIndex % Byte.SIZE)));
                }
                bitIndex++;
            }
        }

        return HEX.formatHex(hashBytes);
    }

    private static byte[] sha256Bytes(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static int hexToNibble(char c) {
        int value = Character.digit(c, 16);
        if (value < 0) {
            throw new IllegalArgumentException("Invalid hexadecimal character: " + c);
        }

        return value;
    }

    public record SkinFingerprint(String sha256, String perceptualHash, SkinVariant variant) {
    }

    private record NormalizedSkin(BufferedImage image, SkinVariant variant) {
    }
}
