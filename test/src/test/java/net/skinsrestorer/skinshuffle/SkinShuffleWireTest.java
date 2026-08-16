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
package net.skinsrestorer.skinshuffle;

import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.integration.skinshuffle.SkinShuffleWire;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkinShuffleWireTest {
    @Test
    void shouldDecodeSkinRefreshPayloadRoundTrip() {
        SkinProperty property = SkinProperty.of("value", "signature");

        byte[] encoded = SkinShuffleWire.createSkinRefreshPayload(property);

        assertEquals(property, SkinShuffleWire.decodeSkinRefreshPayload(encoded).orElseThrow());
    }

    @Test
    void shouldRejectUnsignedSkinRefreshPayload() {
        byte[] encoded = {
                0,
                8, 't', 'e', 'x', 't', 'u', 'r', 'e', 's',
                5, 'v', 'a', 'l', 'u', 'e'
        };

        assertTrue(SkinShuffleWire.decodeSkinRefreshPayload(encoded).isEmpty());
    }

    @Test
    void shouldEncodeRefreshPlayerListEntryAsVarInt() {
        byte[] encoded = SkinShuffleWire.createRefreshPlayerListEntryPayload(300);

        assertArrayEquals(new byte[]{(byte) 0xAC, 0x02}, encoded);
        assertEquals(300, SkinShuffleWire.decodeRefreshPlayerListEntryPayload(encoded).orElseThrow());
    }

    @Test
    void shouldRejectRefreshPlayerListEntryVarIntLargerThan32Bits() {
        byte[] encoded = {(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, 0x10};

        assertTrue(SkinShuffleWire.decodeRefreshPlayerListEntryPayload(encoded).isEmpty());
    }
}
