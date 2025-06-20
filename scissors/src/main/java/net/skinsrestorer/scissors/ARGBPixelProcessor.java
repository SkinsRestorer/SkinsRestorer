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

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.function.IntUnaryOperator;

public interface ARGBPixelProcessor extends IntUnaryOperator {
    static ARGBPixelProcessor of(IntUnaryOperator op) {
        return op instanceof ARGBPixelProcessor processor ? processor : op::applyAsInt;
    }

    @Override
    int applyAsInt(int argbPixel);

    @Override
    default @NotNull ARGBPixelProcessor compose(@NotNull IntUnaryOperator before) {
        return pixel -> applyAsInt(before.applyAsInt(pixel));
    }

    @Override
    default @NotNull ARGBPixelProcessor andThen(@NotNull IntUnaryOperator after) {
        return pixel -> after.applyAsInt(applyAsInt(pixel));
    }

    static @NotNull ARGBPixelProcessor identity() {
        return pixel -> pixel;
    }

    static ARGBPixelProcessor grayscale() {
        return pixel -> {
            ARGB argb = new ARGB(pixel);
            int gray = (argb.red + argb.green + argb.blue) / 3;
            return new ARGB(argb.alpha, gray, gray, gray).toInt();
        };
    }

    static ARGBPixelProcessor invert() {
        return pixel -> {
            ARGB argb = new ARGB(pixel);
            return new ARGB(argb.alpha, 255 - argb.red, 255 - argb.green, 255 - argb.blue).toInt();
        };
    }

    static ARGBPixelProcessor alphaTint(int tint) {
        return pixel -> {
            ARGB argb = new ARGB(pixel);
            int alpha = (argb.alpha * tint) / 255;
            return new ARGB(alpha, argb.red, argb.green, argb.blue).toInt();
        };
    }

    static ARGBPixelProcessor redTint(int tint) {
        return pixel -> {
            ARGB argb = new ARGB(pixel);
            int red = (argb.red * tint) / 255;
            return new ARGB(argb.alpha, red, argb.green, argb.blue).toInt();
        };
    }

    static ARGBPixelProcessor greenTint(int tint) {
        return pixel -> {
            ARGB argb = new ARGB(pixel);
            int green = (argb.green * tint) / 255;
            return new ARGB(argb.alpha, argb.red, green, argb.blue).toInt();
        };
    }

    static ARGBPixelProcessor blueTint(int tint) {
        return pixel -> {
            ARGB argb = new ARGB(pixel);
            int blue = (argb.blue * tint) / 255;
            return new ARGB(argb.alpha, argb.red, argb.green, blue).toInt();
        };
    }

    static ARGBPixelProcessor adjustHue(float hueAdjustment) {
        return pixel -> {
            HSBA hsba = new HSBA(pixel);
            float newHue = (hsba.hue + hueAdjustment) % 1.0f;
            if (newHue < 0) newHue += 1.0f; // Ensure hue is in [0, 1)
            return new HSBA(newHue, hsba.saturation, hsba.brightness, hsba.alpha).toARGB();
        };
    }

    static ARGBPixelProcessor adjustSaturation(float saturationFactor) {
        return pixel -> {
            HSBA hsba = new HSBA(pixel);
            float newSaturation = Math.min(Math.max(hsba.saturation * saturationFactor, 0), 1);
            return new HSBA(hsba.hue, newSaturation, hsba.brightness, hsba.alpha).toARGB();
        };
    }

    static ARGBPixelProcessor adjustBrightness(float brightnessFactor) {
        return pixel -> {
            HSBA hsba = new HSBA(pixel);
            float newBrightness = Math.min(Math.max(hsba.brightness * brightnessFactor, 0), 1);
            return new HSBA(hsba.hue, hsba.saturation, newBrightness, hsba.alpha).toARGB();
        };
    }

    record ARGB(int alpha, int red, int green, int blue) {
        public ARGB(int argb) {
            this((argb >> 24) & 0xFF, (argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
        }

        public int toInt() {
            return (alpha << 24) | (red << 16) | (green << 8) | blue;
        }
    }

    record HSBA(float hue, float saturation, float brightness, int alpha) {
        public HSBA(int argb) {
            this(new ARGB(argb));
        }

        public HSBA(ARGB argb) {
            this(Color.RGBtoHSB(argb.red, argb.green, argb.blue, null), argb.alpha);
        }

        public HSBA(float[] hsb, int alpha) {
            this(hsb[0], hsb[1], hsb[2], alpha);
        }

        public int toARGB() {
            int rgb = Color.HSBtoRGB(hue, saturation, brightness);
            return new ARGB(alpha, (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF).toInt();
        }
    }
}
