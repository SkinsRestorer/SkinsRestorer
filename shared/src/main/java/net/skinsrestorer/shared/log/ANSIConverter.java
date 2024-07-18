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

import net.skinsrestorer.shared.utils.SRHelpers;

import java.util.Map;

public class ANSIConverter {
    private static final Map<SRChatColor, String> replacements = SRHelpers.suppliedMap(SRChatColor.class, e -> switch (e) {
        case BLACK -> "\u001B[30;22m";
        case DARK_BLUE -> "\u001B[34;22m";
        case DARK_GREEN -> "\u001B[32;22m";
        case DARK_AQUA -> "\u001B[36;22m";
        case DARK_RED -> "\u001B[31;22m";
        case DARK_PURPLE -> "\u001B[35;22m";
        case GOLD -> "\u001B[33;22m";
        case GRAY -> "\u001B[37;22m";
        case DARK_GRAY -> "\u001B[30;1m";
        case BLUE -> "\u001B[34;1m";
        case GREEN -> "\u001B[32;1m";
        case AQUA -> "\u001B[36;1m";
        case RED -> "\u001B[31;1m";
        case LIGHT_PURPLE -> "\u001B[35;1m";
        case YELLOW -> "\u001B[33;1m";
        case WHITE -> "\u001B[37;1m";
        case MAGIC -> "\u001B[5m";
        case BOLD -> "\u001B[21m";
        case STRIKETHROUGH -> "\u001B[9m";
        case UNDERLINE -> "\u001B[4m";
        case ITALIC -> "\u001B[3m";
        case RESET -> "\u001B[0;39m";
    });
    private static final SRChatColor[] colors = SRChatColor.values();

    public static String convertToAnsi(String minecraftMessage) {
        String result = minecraftMessage;
        for (SRChatColor color : colors) {
            result = result.replaceAll("(?i)" + color.toString(),
                    replacements.getOrDefault(color, ""));
        }
        return result;
    }
}
