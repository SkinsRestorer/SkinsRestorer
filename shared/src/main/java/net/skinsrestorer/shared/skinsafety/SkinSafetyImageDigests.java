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

import java.util.EnumMap;
import java.util.Map;

public record SkinSafetyImageDigests(Map<SkinSafetyDigestAlgorithm, String> digests) {
    public SkinSafetyImageDigests {
        digests = Map.copyOf(digests);
    }

    public static SkinSafetyImageDigests of(String sha256Digest, String perceptualDigest) {
        Map<SkinSafetyDigestAlgorithm, String> digests = new EnumMap<>(SkinSafetyDigestAlgorithm.class);
        digests.put(SkinSafetyDigestAlgorithm.SHA256, SkinSafetyDigestAlgorithm.normalizeDigestValue(sha256Digest));
        digests.put(SkinSafetyDigestAlgorithm.PERCEPTUAL_V1, SkinSafetyDigestAlgorithm.normalizeDigestValue(perceptualDigest));
        return new SkinSafetyImageDigests(digests);
    }

    public String getDigest(SkinSafetyDigestAlgorithm algorithm) {
        return digests.get(algorithm);
    }
}
