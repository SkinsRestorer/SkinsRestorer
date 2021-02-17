package net.skinsrestorer.shared.utils;

import java.util.regex.Pattern;

public class SharedMethods {
    public static void allowIllegalACFNames() {
        try {
            Class<?> patternClass = Class.forName("co.aikar.commands.ACFPatterns");

            ReflectionUtil.setObject(patternClass, null, "VALID_NAME_PATTERN", Pattern.compile("(.*?)"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
