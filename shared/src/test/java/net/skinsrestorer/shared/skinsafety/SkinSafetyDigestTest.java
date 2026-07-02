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

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SkinSafetyDigestTest {
    @Test
    void shouldParseAlgorithmPrefixedDigests() {
        assertEquals(
                Optional.of(SkinSafetyDigest.sha256("aabbcc")),
                SkinSafetyDigest.parse("SHA256:AABBCC")
        );
        assertEquals(
                Optional.of(SkinSafetyDigest.perceptualV1("00ff")),
                SkinSafetyDigest.parse("perceptual-v1:00FF")
        );
    }

    @Test
    void shouldRejectInvalidDigestFormats() {
        assertTrue(SkinSafetyDigest.parse("").isEmpty());
        assertTrue(SkinSafetyDigest.parse("aabbcc").isEmpty());
        assertTrue(SkinSafetyDigest.parse("unknown:aabbcc").isEmpty());
        assertTrue(SkinSafetyDigest.parse("sha256:").isEmpty());
    }

    @Test
    void shouldEncodeNormalizedDigests() {
        assertEquals("sha256:aabbcc", SkinSafetyDigest.sha256("AABBCC").encoded());
        assertEquals("perceptual-v1:00ff", SkinSafetyDigest.perceptualV1("00FF").encoded());
    }

    @Test
    void shouldRequireExactSha256Matches() {
        assertTrue(SkinSafetyDigestAlgorithm.SHA256.matches("aabb", "aabb", 0));
        assertFalse(SkinSafetyDigestAlgorithm.SHA256.matches("aabb", "aaab", 4));
    }

    @Test
    void shouldRespectPerceptualDistanceThreshold() {
        assertTrue(SkinSafetyDigestAlgorithm.PERCEPTUAL_V1.matches("00", "01", 1));
        assertFalse(SkinSafetyDigestAlgorithm.PERCEPTUAL_V1.matches("00", "01", 0));
    }
}
