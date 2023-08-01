package net.skinsrestorer.shared.utils;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
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
