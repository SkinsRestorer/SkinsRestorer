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

import ch.jalu.configme.SettingsManager;
import net.skinsrestorer.api.Base64Utils;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.config.MessageConfig;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.messages.ComponentHelper;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

public class SRHelpers {
    public static final SkinProperty EMPTY_SKIN = SkinProperty.of("", "");
    public static final String MESSAGE_CHANNEL = "sr:messagechannel";
    private static final String NAMEMC_IMG_URL = "https://s.namemc.com/i/%s.png";

    private SRHelpers() {
    }

    public static Throwable getRootCause(Throwable throwable) {
        if (throwable.getCause() != null) {
            return getRootCause(throwable.getCause());
        }

        return throwable;
    }

    public static byte[] hashSHA256ToBytes(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static long hashSha256ToLong(byte[] bytes) {
        return ByteBuffer.wrap(hashSHA256ToBytes(bytes)).getLong();
    }

    public static long hashSha256ToLong(String str) {
        return hashSha256ToLong(str.getBytes(StandardCharsets.UTF_8));
    }

    public static String hashSha256ToHex(byte[] bytes) {
        return bytesToHex(hashSHA256ToBytes(bytes));
    }

    public static String hashSha256ToHex(String str) {
        return hashSha256ToHex(str.getBytes(StandardCharsets.UTF_8));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static long getEpochSecond() {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    }

    public static void renameFile(Path parent, String oldName, String newName) throws IOException {
        try (Stream<Path> stream = Files.list(parent)) {
            // Folders are case-insensitive on Windows, so we need to check it using this method
            List<String> files = stream.map(Path::getFileName).map(Path::toString).toList();

            String tempName = "%s_temp".formatted(newName);
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
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    public static <E> E getRandomEntry(Collection<E> list) {
        int index = ThreadLocalRandom.current().nextInt(list.size());
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
            throw new IllegalArgumentException("Invalid Java version: %s".formatted(specVersion));
        } else if (split.length == 1) {
            majorVersion = split[0];
        } else if (split[0].equals("1")) {
            majorVersion = split[1];
        } else {
            throw new IllegalArgumentException("Invalid Java version: %s".formatted(specVersion));
        }

        return Integer.parseInt(majorVersion);
    }

    public static Optional<URL> parseURL(String str) {
        try {
            return Optional.of(URI.create(str).toURL());
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
                return NAMEMC_IMG_URL.formatted(uuid);
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

    public static String formatEpochSeconds(SettingsManager settings, long epochSeconds, Locale locale) {
        return formatEpochMillis(settings, TimeUnit.SECONDS.toMillis(epochSeconds), locale);
    }

    public static String formatEpochMillis(SettingsManager settings, long epochMillis, Locale locale) {
        return new SimpleDateFormat(settings.getProperty(MessageConfig.DATE_FORMAT), locale)
                .format(new Date(epochMillis));
    }

    public static <E extends Enum<E>, V> Map<E, V> suppliedMap(Class<E> clazz, Function<E, V> mapper) {
        Map<E, V> map = new EnumMap<>(clazz);
        for (E e : clazz.getEnumConstants()) {
            map.put(e, mapper.apply(e));
        }

        return map;
    }

    public static boolean isNotAllowedUnquotedString(String str) {
        return !str.chars().allMatch(c -> isAllowedInUnquotedString((char) c));
    }

    public static boolean isAllowedInUnquotedString(char c) {
        return c >= '0' && c <= '9'
                || c >= 'A' && c <= 'Z'
                || c >= 'a' && c <= 'z'
                || c == '_' || c == '-'
                || c == '.' || c == '+';
    }

    public static String durationFormat(SkinsRestorerLocale locale, SRCommandSender sender, Duration duration) {
        long days = duration.toDaysPart();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        StringBuilder result = new StringBuilder();
        if (days > 0) {
            result.append(days)
                    .append(ComponentHelper.convertJsonToPlain(locale.getMessageRequired(sender, days == 1 ? Message.DURATION_DAY : Message.DURATION_DAYS)))
                    .append(" ");
        }
        if (hours > 0) {
            result.append(hours)
                    .append(ComponentHelper.convertJsonToPlain(locale.getMessageRequired(sender, hours == 1 ? Message.DURATION_HOUR : Message.DURATION_HOURS)))
                    .append(" ");
        }
        if (minutes > 0) {
            result.append(minutes)
                    .append(ComponentHelper.convertJsonToPlain(locale.getMessageRequired(sender, minutes == 1 ? Message.DURATION_MINUTE : Message.DURATION_MINUTES)))
                    .append(" ");
        }
        if (seconds > 0 || result.isEmpty()) {
            result.append(seconds)
                    .append(ComponentHelper.convertJsonToPlain(locale.getMessageRequired(sender, seconds == 1 ? Message.DURATION_SECOND : Message.DURATION_SECONDS)))
                    .append(" ");
        }

        return result.toString().trim();
    }

    @SuppressWarnings("HttpUrlsUsage")
    public static String encodeHashToTexturesValue(String textureHash) {
        return Base64Utils.encode("{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/%s\"}}}".formatted(textureHash));
    }

    public static void createDirectoriesSafe(Path path) {
        if (!Files.isDirectory(path)) { // In case the directory is a symbol link
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create directories: %s".formatted(path), e);
            }
        }
    }

    public static void writeIfNeeded(Path path, String content) throws IOException {
        if (Files.exists(path)) {
            var existingContent = Files.readString(path);
            if (!existingContent.equals(content)) {
                Files.writeString(path, content);
            }
        } else {
            SRHelpers.createDirectoriesSafe(path.getParent());
            Files.writeString(path, content);
        }
    }
}
