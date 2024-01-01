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

import java.util.EnumMap;
import java.util.Map;

public class ANSIConverter {
    private static final Map<SRChatColor, String> replacements = new EnumMap<>(SRChatColor.class);
    private static final SRChatColor[] colors = SRChatColor.values();

    static {
        replacements.put(SRChatColor.BLACK, AnsiBuilder.ansi().fg(AnsiBuilder.Color.BLACK).boldOff().toString());
        replacements.put(SRChatColor.DARK_BLUE, AnsiBuilder.ansi().fg(AnsiBuilder.Color.BLUE).boldOff().toString());
        replacements.put(SRChatColor.DARK_GREEN, AnsiBuilder.ansi().fg(AnsiBuilder.Color.GREEN).boldOff().toString());
        replacements.put(SRChatColor.DARK_AQUA, AnsiBuilder.ansi().fg(AnsiBuilder.Color.CYAN).boldOff().toString());
        replacements.put(SRChatColor.DARK_RED, AnsiBuilder.ansi().fg(AnsiBuilder.Color.RED).boldOff().toString());
        replacements.put(SRChatColor.DARK_PURPLE, AnsiBuilder.ansi().fg(AnsiBuilder.Color.MAGENTA).boldOff().toString());
        replacements.put(SRChatColor.GOLD, AnsiBuilder.ansi().fg(AnsiBuilder.Color.YELLOW).boldOff().toString());
        replacements.put(SRChatColor.GRAY, AnsiBuilder.ansi().fg(AnsiBuilder.Color.WHITE).boldOff().toString());
        replacements.put(SRChatColor.DARK_GRAY, AnsiBuilder.ansi().fg(AnsiBuilder.Color.BLACK).bold().toString());
        replacements.put(SRChatColor.BLUE, AnsiBuilder.ansi().fg(AnsiBuilder.Color.BLUE).bold().toString());
        replacements.put(SRChatColor.GREEN, AnsiBuilder.ansi().fg(AnsiBuilder.Color.GREEN).bold().toString());
        replacements.put(SRChatColor.AQUA, AnsiBuilder.ansi().fg(AnsiBuilder.Color.CYAN).bold().toString());
        replacements.put(SRChatColor.RED, AnsiBuilder.ansi().fg(AnsiBuilder.Color.RED).bold().toString());
        replacements.put(SRChatColor.LIGHT_PURPLE, AnsiBuilder.ansi().fg(AnsiBuilder.Color.MAGENTA).bold().toString());
        replacements.put(SRChatColor.YELLOW, AnsiBuilder.ansi().fg(AnsiBuilder.Color.YELLOW).bold().toString());
        replacements.put(SRChatColor.WHITE, AnsiBuilder.ansi().fg(AnsiBuilder.Color.WHITE).bold().toString());
        replacements.put(SRChatColor.MAGIC, AnsiBuilder.ansi().a(AnsiBuilder.Attribute.BLINK_SLOW).toString());
        replacements.put(SRChatColor.BOLD, AnsiBuilder.ansi().a(AnsiBuilder.Attribute.UNDERLINE_DOUBLE).toString());
        replacements.put(SRChatColor.STRIKETHROUGH, AnsiBuilder.ansi().a(AnsiBuilder.Attribute.STRIKETHROUGH_ON).toString());
        replacements.put(SRChatColor.UNDERLINE, AnsiBuilder.ansi().a(AnsiBuilder.Attribute.UNDERLINE).toString());
        replacements.put(SRChatColor.ITALIC, AnsiBuilder.ansi().a(AnsiBuilder.Attribute.ITALIC).toString());
        replacements.put(SRChatColor.RESET, AnsiBuilder.ansi().a(AnsiBuilder.Attribute.RESET).fg(AnsiBuilder.Color.DEFAULT).toString());
    }

    public static String convertToAnsi(String minecraftMessage) {
        String result = minecraftMessage;
        for (SRChatColor color : colors) {
            result = result.replaceAll("(?i)" + color.toString(),
                    replacements.getOrDefault(color, ""));
        }
        return result;
    }
}
