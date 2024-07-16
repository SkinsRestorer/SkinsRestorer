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
import net.skinsrestorer.shared.gui.PageType;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MessageProtocolUtil {
    private static <T> List<T> readList(DataInputStream is, ThrowingSupplier<T> reader) {
        try {
            int size = is.readInt();
            List<T> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(reader.get());
            }

            return list;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> void writeList(DataOutputStream os, List<T> list, ThrowingConsumer<T> writer) {
        try {
            os.writeInt(list.size());
            for (T entry : list) {
                writer.accept(entry);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static PageInfo convertToPageInfo(byte[] byteArr) {
        try {
            DataInputStream is = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(byteArr)));

            int page = is.readInt();
            PageType pageType = PageType.fromKey(is.readUTF()).orElseThrow();
            boolean hasPrevious = is.readBoolean();
            boolean hasNext = is.readBoolean();
            List<GUISkinEntry> skinList = readList(is, () -> {
                String skinId = is.readUTF();
                String skinName = is.readUTF();
                String textureHash = is.readUTF();
                GUIUtils.GUIRawSkinEntry base = new GUIUtils.GUIRawSkinEntry(skinId, skinName, textureHash);

                List<ComponentString> lore = readList(is, () -> new ComponentString(is.readUTF()));

                return new GUISkinEntry(base, lore);
            });

            return new PageInfo(page, pageType, hasPrevious, hasNext, skinList);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] convertToByteArray(PageInfo pageInfo) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

        try (GZIPOutputStream gzipOut = new GZIPOutputStream(byteOut)) {
            DataOutputStream os = new DataOutputStream(gzipOut);

            os.writeInt(pageInfo.page());
            os.writeUTF(pageInfo.pageType().getKey());
            os.writeBoolean(pageInfo.hasPrevious());
            os.writeBoolean(pageInfo.hasNext());
            writeList(os, pageInfo.skinList(), (entry) -> {
                os.writeUTF(entry.base().skinId());
                os.writeUTF(entry.base().skinName());
                os.writeUTF(entry.base().textureHash());
                writeList(os, entry.lore(), (component) -> os.writeUTF(component.jsonString()));
            });

            os.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return byteOut.toByteArray();
    }

    private interface ThrowingSupplier<T> {
        T get() throws IOException;
    }

    private interface ThrowingConsumer<T> {
        void accept(T t) throws IOException;
    }
}
