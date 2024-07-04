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

import net.skinsrestorer.shared.gui.GUISkinEntry;
import net.skinsrestorer.shared.gui.PageInfo;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MessageProtocolUtil {
    public static PageInfo convertToPageInfo(byte[] byteArr) {
        try {
            DataInputStream ois = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(byteArr)));

            boolean hasNext = ois.readBoolean();
            int size = ois.readInt();
            List<GUISkinEntry> skinList = new ArrayList<>(size);

            for (int i = 0; i < size; i++) {
                String skinId = ois.readUTF();
                String skinName = ois.readUTF();
                String textureHash = ois.readUTF();

                skinList.add(new GUISkinEntry(skinId, skinName, textureHash));
            }

            return new PageInfo(hasNext, skinList);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] convertToByteArray(PageInfo pageInfo) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

        try (GZIPOutputStream gzipOut = new GZIPOutputStream(byteOut)) {
            DataOutputStream dataOut = new DataOutputStream(gzipOut);
            dataOut.writeBoolean(pageInfo.hasNext());
            dataOut.writeInt(pageInfo.skinList().size());
            for (GUISkinEntry entry : pageInfo.skinList()) {
                dataOut.writeUTF(entry.skinId());
                dataOut.writeUTF(entry.skinName());
                dataOut.writeUTF(entry.textureHash());
            }

            dataOut.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return byteOut.toByteArray();
    }
}
