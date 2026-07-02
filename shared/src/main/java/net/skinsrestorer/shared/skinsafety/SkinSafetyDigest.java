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

import java.util.Objects;
import java.util.Optional;

public record SkinSafetyDigest(SkinSafetyDigestAlgorithm algorithm, String value) {
    public SkinSafetyDigest {
        Objects.requireNonNull(algorithm, "algorithm");
        Objects.requireNonNull(value, "value");
        value = SkinSafetyDigestAlgorithm.normalizeDigestValue(value);
    }

    public static Optional<SkinSafetyDigest> parse(String encodedDigest) {
        if (encodedDigest == null || encodedDigest.isBlank()) {
            return Optional.empty();
        }

        int separatorIndex = encodedDigest.indexOf(':');
        if (separatorIndex <= 0 || separatorIndex == encodedDigest.length() - 1) {
            return Optional.empty();
        }

        Optional<SkinSafetyDigestAlgorithm> algorithm = SkinSafetyDigestAlgorithm.fromId(encodedDigest.substring(0, separatorIndex));
        if (algorithm.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new SkinSafetyDigest(algorithm.get(), encodedDigest.substring(separatorIndex + 1)));
    }

    public static SkinSafetyDigest sha256(String value) {
        return new SkinSafetyDigest(SkinSafetyDigestAlgorithm.SHA256, value);
    }

    public static SkinSafetyDigest perceptualV1(String value) {
        return new SkinSafetyDigest(SkinSafetyDigestAlgorithm.PERCEPTUAL_V1, value);
    }

    public String encoded() {
        return algorithm.encode(value);
    }
}
