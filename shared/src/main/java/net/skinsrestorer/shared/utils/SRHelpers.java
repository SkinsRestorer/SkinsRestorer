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

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class SRHelpers {
    private static final String NAMEMC_IMG_URL = "https://s.namemc.com/i/%s.png";

    private SRHelpers() {
    }

    public static Throwable getRootCause(Throwable throwable) {
        if (throwable.getCause() != null) {
            return getRootCause(throwable.getCause());
        }

        return throwable;
    }

    public static long hashSha256String(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(str.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            return ByteBuffer.wrap(digest).getLong();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to get SHA-256 hash algorithm", e);
        }
    }

    public static byte[] md5(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(input);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static long getEpochSecond() {
        return System.currentTimeMillis() / 1000L;
    }

    public static void renameFile(Path parent, String oldName, String newName) throws IOException {
        try (Stream<Path> stream = Files.list(parent)) {
            // Folders are case-insensitive on Windows, so we need to check it using this method
            List<String> files = stream.map(Path::getFileName).map(Path::toString).toList();

            String tempName = newName + "_temp";
            if (files.contains(oldName) && !files.contains(tempName) && !files.contains(newName)) {
                Path oldPath = parent.resolve(oldName);
                Path tempPath = parent.resolve(tempName);
                Path newPath = parent.resolve(newName);

                // Windows will not allow renaming a folder to a name that differs only in case
                // So we need to rename it to a temporary name first
                Files.move(oldPath, tempPath, StandardCopyOption.REPLACE_EXISTING);
                Files.move(tempPath, newPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public static <E> E getRandomEntry(List<E> list) {
        Random random = ThreadLocalRandom.current();
        return list.get(random.nextInt(list.size()));
    }

    public static <E> E getRandomEntry(Collection<E> list) {
        Random random = ThreadLocalRandom.current();
        int index = random.nextInt(list.size());
        int i = 0;
        for (E entry : list) {
            if (i == index) {
                return entry;
            }
            i++;
        }

        throw new IllegalStateException("Failed to get random entry");
    }

    public static int getJavaVersion() {
        String specVersion = System.getProperty("java.specification.version");
        String[] split = specVersion.split("\\.");

        String majorVersion;
        if (split.length == 0 || split.length > 2) {
            throw new IllegalArgumentException("Invalid Java version: " + specVersion);
        } else if (split.length == 1) {
            majorVersion = split[0];
        } else if (split[0].equals("1")) {
            majorVersion = split[1];
        } else {
            throw new IllegalArgumentException("Invalid Java version: " + specVersion);
        }

        return Integer.parseInt(majorVersion);
    }

    public static Optional<URL> parseURL(String str) {
        try {
            return Optional.of(new URL(str));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static String sanitizeImageURL(String imageUrl) {
        Optional<URL> uriOptional = parseURL(imageUrl);
        if (uriOptional.isEmpty()) {
            return imageUrl;
        }

        String host = uriOptional.get().getHost();

        if (host == null) {
            return imageUrl;
        }

        boolean isNamemc = host.equals("namemc.com") || host.endsWith(".namemc.com");
        if (isNamemc) {
            String path = uriOptional.get().getPath();
            if (path == null) {
                return imageUrl;
            }

            String skinPath = "/skin/";
            if (path.startsWith(skinPath)) {
                String uuid = path.substring(skinPath.length());
                return String.format(NAMEMC_IMG_URL, uuid);
            }
        }

        return imageUrl;
    }

    public static String sanitizeSkinInput(String skinInput) {
        Optional<URL> uriOptional = parseURL(skinInput);
        if (uriOptional.isEmpty()) {
            return skinInput;
        }

        String host = uriOptional.get().getHost();
        if (host == null) {
            return skinInput;
        }

        boolean isNamemc = host.equals("namemc.com") || host.endsWith(".namemc.com");
        if (isNamemc) {
            String path = uriOptional.get().getPath();
            if (path == null) {
                return skinInput;
            }

            String profilePath = "/profile/";
            if (path.startsWith(profilePath)) {
                String usernamePart = path.substring(profilePath.length());
                int dotIndex = usernamePart.indexOf('.');
                if (dotIndex != -1) {
                    usernamePart = usernamePart.substring(0, dotIndex);
                }

                if (ValidationUtil.validSkinUrl(usernamePart)) {
                    return usernamePart;
                }
            }
        }

        return skinInput;
    }
}
