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
package net.skinsrestorer.shared.skinsafety;

import net.skinsrestorer.scissors.skin.SkinHashing;

import java.util.Locale;
import java.util.Optional;

public enum SkinSafetyDigestAlgorithm {
    SHA256("sha256"),
    PERCEPTUAL_V1("perceptual-v1");

    private final String id;

    SkinSafetyDigestAlgorithm(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String encode(String digestValue) {
        return id + ":" + normalizeDigestValue(digestValue);
    }

    public boolean matches(String candidateDigest, String blockedDigest, int perceptualHashMaxDistance) {
        return switch (this) {
            case SHA256 -> normalizeDigestValue(candidateDigest).equals(normalizeDigestValue(blockedDigest));
            case PERCEPTUAL_V1 -> SkinHashing.hammingDistance(
                    normalizeDigestValue(candidateDigest),
                    normalizeDigestValue(blockedDigest)
            ) <= perceptualHashMaxDistance;
        };
    }

    public static Optional<SkinSafetyDigestAlgorithm> fromId(String rawId) {
        String normalizedId = normalizeDigestValue(rawId);
        for (SkinSafetyDigestAlgorithm algorithm : values()) {
            if (algorithm.id.equals(normalizedId)) {
                return Optional.of(algorithm);
            }
        }
        return Optional.empty();
    }

    static String normalizeDigestValue(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
