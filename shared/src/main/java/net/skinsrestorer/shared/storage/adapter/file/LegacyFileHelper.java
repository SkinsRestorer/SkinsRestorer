package net.skinsrestorer.shared.storage.adapter.file;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class LegacyFileHelper {
    private static final Pattern FORBIDDEN_CHARS_PATTERN = Pattern.compile("[\\\\/:*?\"<>|.]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");

    static Optional<SkinStorageType> readLegacySkinFile(Path file) {
        if (!Files.exists(file))
            return Optional.empty();

        try {
            List<String> lines = Files.readAllLines(file);
            SkinStorageType type = new SkinStorageType(lines.get(0), lines.get(1), Long.parseLong(lines.get(2)));
            if (type.isInvalid())
                return Optional.empty();

            return Optional.of(type);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    static Optional<PlayerStorageType> readLegacyPlayerFile(Path file) {
        if (!Files.exists(file))
            return Optional.empty();

        try {
            List<String> lines = Files.readAllLines(file);
            PlayerStorageType type = new PlayerStorageType(lines.get(0));
            if (type.isInvalid())
                return Optional.empty();

            return Optional.of(type);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    static Path resolveLegacySkinFile(Path skinsFolder, String skinName) {
        skinName = removeWhitespaces(skinName);
        skinName = replaceForbiddenChars(skinName);
        return skinsFolder.resolve(skinName + ".skin");
    }

    static Path resolveLegacyPlayerFile(Path playersFolder, String playerName) {
        playerName = replaceForbiddenChars(playerName);
        return playersFolder.resolve(playerName + ".player");
    }

    static String replaceForbiddenChars(String str) {
        // Escape all Windows / Linux forbidden printable ASCII characters
        return FORBIDDEN_CHARS_PATTERN.matcher(str).replaceAll("·");
    }

    // TODO remove all whitespace after last starting space.
    static String removeWhitespaces(String str) {
        // Remove all whitespace expect when startsWith " ".
        if (str.startsWith(" ")) {
            return str;
        }
        return WHITESPACE_PATTERN.matcher(str).replaceAll("");
    }
}
