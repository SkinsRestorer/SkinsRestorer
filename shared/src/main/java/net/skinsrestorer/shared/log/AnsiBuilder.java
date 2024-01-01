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
package net.skinsrestorer.shared.log;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;

/**
 * This class is a copy of <a href="https://github.com/fusesource/jansi/blob/master/src/main/java/org/fusesource/jansi/Ansi.java">The Jansi project</a>
 * Stripped down, so it only contains the methods we need and no natives.
 * <p>
 * Provides a fluent API for generating
 * <a href="https://en.wikipedia.org/wiki/ANSI_escape_code#CSI_sequences">ANSI escape sequences</a>.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class AnsiBuilder implements Appendable {
    private static final char FIRST_ESC_CHAR = 27;
    private static final char SECOND_ESC_CHAR = '[';
    private final StringBuilder builder;
    private final ArrayList<Integer> attributeOptions = new ArrayList<>(5);

    public AnsiBuilder() {
        this(new StringBuilder(80));
    }

    public AnsiBuilder(StringBuilder builder) {
        this.builder = builder;
    }

    public static AnsiBuilder ansi() {
        return new AnsiBuilder();
    }

    public AnsiBuilder fg(Color color) {
        attributeOptions.add(color.fg());
        return this;
    }

    public AnsiBuilder a(Attribute attribute) {
        attributeOptions.add(attribute.value());
        return this;
    }

    public AnsiBuilder bold() {
        return a(Attribute.INTENSITY_BOLD);
    }

    public AnsiBuilder boldOff() {
        return a(Attribute.INTENSITY_BOLD_OFF);
    }

    @Override
    public String toString() {
        flushAttributes();
        return builder.toString();
    }

    private void flushAttributes() {
        if (attributeOptions.isEmpty())
            return;
        if (attributeOptions.size() == 1 && attributeOptions.get(0) == 0) {
            builder.append(FIRST_ESC_CHAR);
            builder.append(SECOND_ESC_CHAR);
            builder.append('m');
        } else {
            _appendEscapeSequence(attributeOptions.toArray());
        }
        attributeOptions.clear();
    }

    private void _appendEscapeSequence(Object... options) {
        builder.append(FIRST_ESC_CHAR);
        builder.append(SECOND_ESC_CHAR);
        int size = options.length;
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                builder.append(';');
            }
            if (options[i] != null) {
                builder.append(options[i]);
            }
        }
        builder.append('m');
    }

    @Override
    public AnsiBuilder append(CharSequence csq) {
        builder.append(csq);
        return this;
    }

    @Override
    public AnsiBuilder append(CharSequence csq, int start, int end) {
        builder.append(csq, start, end);
        return this;
    }

    @Override
    public AnsiBuilder append(char c) {
        builder.append(c);
        return this;
    }

    /**
     * <a href="https://en.wikipedia.org/wiki/ANSI_escape_code#Colors">ANSI 8 colors</a> for fluent API
     */
    @RequiredArgsConstructor
    public enum Color {
        BLACK(0, "BLACK"),
        RED(1, "RED"),
        GREEN(2, "GREEN"),
        YELLOW(3, "YELLOW"),
        BLUE(4, "BLUE"),
        MAGENTA(5, "MAGENTA"),
        CYAN(6, "CYAN"),
        WHITE(7, "WHITE"),
        DEFAULT(9, "DEFAULT");

        private final int value;
        private final String name;

        @Override
        public String toString() {
            return name;
        }

        public int value() {
            return value;
        }

        public int fg() {
            return value + 30;
        }
    }

    /**
     * Display attributes, also know as
     * <a href="https://en.wikipedia.org/wiki/ANSI_escape_code#SGR_(Select_Graphic_Rendition)_parameters">SGR
     * (Select Graphic Rendition) parameters</a>.
     */
    @RequiredArgsConstructor
    public enum Attribute {
        RESET(0, "RESET"),
        INTENSITY_BOLD(1, "INTENSITY_BOLD"),
        INTENSITY_FAINT(2, "INTENSITY_FAINT"),
        ITALIC(3, "ITALIC_ON"),
        UNDERLINE(4, "UNDERLINE_ON"),
        BLINK_SLOW(5, "BLINK_SLOW"),
        BLINK_FAST(6, "BLINK_FAST"),
        NEGATIVE_ON(7, "NEGATIVE_ON"),
        CONCEAL_ON(8, "CONCEAL_ON"),
        STRIKETHROUGH_ON(9, "STRIKETHROUGH_ON"),
        UNDERLINE_DOUBLE(21, "UNDERLINE_DOUBLE"),
        INTENSITY_BOLD_OFF(22, "INTENSITY_BOLD_OFF"),
        ITALIC_OFF(23, "ITALIC_OFF"),
        UNDERLINE_OFF(24, "UNDERLINE_OFF"),
        BLINK_OFF(25, "BLINK_OFF"),
        NEGATIVE_OFF(27, "NEGATIVE_OFF"),
        CONCEAL_OFF(28, "CONCEAL_OFF"),
        STRIKETHROUGH_OFF(29, "STRIKETHROUGH_OFF");

        private final int value;
        private final String name;

        @Override
        public String toString() {
            return name;
        }

        public int value() {
            return value;
        }
    }
}
