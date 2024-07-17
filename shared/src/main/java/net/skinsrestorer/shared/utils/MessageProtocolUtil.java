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
package net.skinsrestorer.shared.utils;

import net.skinsrestorer.shared.gui.SRInventory;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MessageProtocolUtil {
    public static SRInventory convertToInventory(byte[] byteArr) {
        try {
            DataInputStream is = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(byteArr)));

            return SRInventory.CODEC.read(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] convertToByteArray(SRInventory srInventory) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

        try (GZIPOutputStream gzipOut = new GZIPOutputStream(byteOut)) {
            DataOutputStream os = new DataOutputStream(gzipOut);

            SRInventory.CODEC.write(os, srInventory);

            os.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return byteOut.toByteArray();
    }
}
