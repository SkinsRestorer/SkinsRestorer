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

public enum SRChatColor {
    /**
     * Represents black
     */
    BLACK('0'),
    /**
     * Represents dark blue
     */
    DARK_BLUE('1'),
    /**
     * Represents dark green
     */
    DARK_GREEN('2'),
    /**
     * Represents dark blue (aqua)
     */
    DARK_AQUA('3'),
    /**
     * Represents dark red
     */
    DARK_RED('4'),
    /**
     * Represents dark purple
     */
    DARK_PURPLE('5'),
    /**
     * Represents gold
     */
    GOLD('6'),
    /**
     * Represents gray
     */
    GRAY('7'),
    /**
     * Represents dark gray
     */
    DARK_GRAY('8'),
    /**
     * Represents blue
     */
    BLUE('9'),
    /**
     * Represents green
     */
    GREEN('a'),
    /**
     * Represents aqua
     */
    AQUA('b'),
    /**
     * Represents red
     */
    RED('c'),
    /**
     * Represents light purple
     */
    LIGHT_PURPLE('d'),
    /**
     * Represents yellow
     */
    YELLOW('e'),
    /**
     * Represents white
     */
    WHITE('f'),
    /**
     * Represents magical characters that change around randomly
     */
    MAGIC('k'),
    /**
     * Makes the text bold.
     */
    BOLD('l'),
    /**
     * Makes a line appear through the text.
     */
    STRIKETHROUGH('m'),
    /**
     * Makes the text appear underlined.
     */
    UNDERLINE('n'),
    /**
     * Makes the text italic.
     */
    ITALIC('o'),
    /**
     * Resets all previous chat colors or formats.
     */
    RESET('r');

    /**
     * The special character which prefixes all chat colour codes. Use this if
     * you need to dynamically convert colour codes from your custom format.
     */
    public static final char COLOR_CHAR = '§';

    private final String toString;

    SRChatColor(char code) {
        this.toString = new String(new char[]{COLOR_CHAR, code});
    }

    @Override
    public String toString() {
        return toString;
    }
}
