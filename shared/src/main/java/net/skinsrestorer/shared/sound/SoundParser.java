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
package net.skinsrestorer.shared.sound;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Pattern;

public class SoundParser {
    public static final float DEFAULT_VOLUME = 1.0f, DEFAULT_PITCH = 1.0f;
    public static final Pattern NAMESPACED_SOUND_PATTERN = Pattern.compile("(?<namespace>[a-z0-9._-]+):(?<key>[a-z0-9/._-]+)");

    private static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private static List<String> split(@Nonnull String str, @SuppressWarnings("SameParameterValue") char separatorChar) {
        List<String> list = new ArrayList<>(4);
        boolean match = false, lastMatch = false;
        int len = str.length();
        int start = 0;

        for (int i = 0; i < len; i++) {
            if (str.charAt(i) == separatorChar) {
                if (match) {
                    list.add(str.substring(start, i));
                    match = false;
                    lastMatch = true;
                }

                // This is important, it should not be i++
                start = i + 1;
                continue;
            }

            lastMatch = false;
            match = true;
        }

        if (match || lastMatch) {
            list.add(str.substring(start, len));
        }
        return list;
    }

    @Nullable
    public static Record parse(@Nullable String sound) {
        if (isNullOrEmpty(sound) || sound.equalsIgnoreCase("none")) return null;
        @SuppressWarnings("DynamicRegexReplaceableByCompiledPattern") List<String> split = split(sound.replace(" ", ""), ',');

        Record record = new Record();
        String name = split.getFirst();
        if (name.charAt(0) == '~') {
            name = name.substring(1);
            record.publicSound(true);
        } else {
            record.publicSound(false);
        }

        if (name.isEmpty()) throw new IllegalArgumentException("No sound name specified: " + sound);
        {
            String soundName;
            int atIndex = name.indexOf('@');
            if (atIndex != -1) {
                String category = name.substring(0, atIndex);
                soundName = name.substring(atIndex + 1);

                String soundCategory = category.toUpperCase(Locale.ENGLISH);
                record.inCategory(soundCategory);
            } else {
                soundName = name;
            }

            if (soundName.isEmpty()) {
                throw new IllegalArgumentException("No sound name specified: " + name);
            }

            record.withSound(soundName.toUpperCase(Locale.ENGLISH));
        }

        try {
            if (split.size() > 1) record.withVolume(Float.parseFloat(split.get(1)));
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("Invalid number '" + split.get(1) + "' for sound volume '" + sound + '\'');
        }
        try {
            if (split.size() > 2) record.withPitch(Float.parseFloat(split.get(2)));
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("Invalid number '" + split.get(2) + "' for sound pitch '" + sound + '\'');
        }

        try {
            if (split.size() > 3) record.withSeed(Long.parseLong(split.get(3)));
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("Invalid number '" + split.get(3) + "' for sound seed '" + sound + '\'');
        }

        return record;
    }

    public static class Record {
        private static final Random RANDOM = new Random();

        private String sound;

        @Nonnull
        private String category = "MASTER";

        @Nullable
        private Long seed;

        private float volume = DEFAULT_VOLUME;
        private float pitch = DEFAULT_PITCH;
        private boolean publicSound;

        @Nullable
        public Long getSeed() {
            return seed;
        }

        public String getSound() {
            return sound;
        }

        @Nonnull
        public String getCategory() {
            return category;
        }

        public float getVolume() {
            return volume;
        }

        public float getPitch() {
            return pitch;
        }

        public void inCategory(String category) {
            this.category = Objects.requireNonNull(category, "Sound category cannot be null");
        }

        public void withSound(@Nonnull String sound) {
            this.sound = Objects.requireNonNull(sound, "Cannot play a null sound").toLowerCase(Locale.ENGLISH);
        }

        public long generateSeed() {
            return seed == null ? RANDOM.nextLong() : seed;
        }

        public void withVolume(float volume) {
            this.volume = volume;
        }

        public void publicSound(boolean publicSound) {
            this.publicSound = publicSound;
        }

        public void withPitch(float pitch) {
            this.pitch = pitch;
        }

        public void withSeed(Long seed) {
            this.seed = seed;
        }
    }
}
