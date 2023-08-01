/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.skinsrestorer.shared.utils;

import java.io.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MessageProtocolUtil {
    public static Map<String, String> convertToMap(byte[] byteArr) {
        try {
            DataInputStream ois = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(byteArr)));

            int size = ois.readInt();
            Map<String, String> map = new LinkedHashMap<>(size);

            for (int i = 0; i < size; i++) {
                String key = ois.readUTF();
                String value = ois.readUTF();
                map.put(key, value);
            }

            return map;
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    public static byte[] convertToByteArray(Map<String, String> map) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

        try (GZIPOutputStream gzipOut = new GZIPOutputStream(byteOut)) {
            DataOutputStream dataOut = new DataOutputStream(gzipOut);
            dataOut.writeInt(map.size());
            for (Map.Entry<String, String> entry : map.entrySet()) {
                dataOut.writeUTF(entry.getKey());
                dataOut.writeUTF(entry.getValue());
            }

            dataOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteOut.toByteArray();
    }
}
