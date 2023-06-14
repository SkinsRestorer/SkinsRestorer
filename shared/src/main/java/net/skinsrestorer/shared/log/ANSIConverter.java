/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.skinsrestorer.shared.log;

import org.fusesource.jansi.Ansi;

import java.util.EnumMap;
import java.util.Map;

import static org.fusesource.jansi.Ansi.Attribute;

public class ANSIConverter {
    private static final Map<SRChatColor, String> replacements = new EnumMap<>(SRChatColor.class);
    private static final SRChatColor[] colors = SRChatColor.values();

    static {
        replacements.put(SRChatColor.BLACK, Ansi.ansi().fg(Ansi.Color.BLACK).boldOff().toString());
        replacements.put(SRChatColor.DARK_BLUE, Ansi.ansi().fg(Ansi.Color.BLUE).boldOff().toString());
        replacements.put(SRChatColor.DARK_GREEN, Ansi.ansi().fg(Ansi.Color.GREEN).boldOff().toString());
        replacements.put(SRChatColor.DARK_AQUA, Ansi.ansi().fg(Ansi.Color.CYAN).boldOff().toString());
        replacements.put(SRChatColor.DARK_RED, Ansi.ansi().fg(Ansi.Color.RED).boldOff().toString());
        replacements.put(SRChatColor.DARK_PURPLE, Ansi.ansi().fg(Ansi.Color.MAGENTA).boldOff().toString());
        replacements.put(SRChatColor.GOLD, Ansi.ansi().fg(Ansi.Color.YELLOW).boldOff().toString());
        replacements.put(SRChatColor.GRAY, Ansi.ansi().fg(Ansi.Color.WHITE).boldOff().toString());
        replacements.put(SRChatColor.DARK_GRAY, Ansi.ansi().fg(Ansi.Color.BLACK).bold().toString());
        replacements.put(SRChatColor.BLUE, Ansi.ansi().fg(Ansi.Color.BLUE).bold().toString());
        replacements.put(SRChatColor.GREEN, Ansi.ansi().fg(Ansi.Color.GREEN).bold().toString());
        replacements.put(SRChatColor.AQUA, Ansi.ansi().fg(Ansi.Color.CYAN).bold().toString());
        replacements.put(SRChatColor.RED, Ansi.ansi().fg(Ansi.Color.RED).bold().toString());
        replacements.put(SRChatColor.LIGHT_PURPLE, Ansi.ansi().fg(Ansi.Color.MAGENTA).bold().toString());
        replacements.put(SRChatColor.YELLOW, Ansi.ansi().fg(Ansi.Color.YELLOW).bold().toString());
        replacements.put(SRChatColor.WHITE, Ansi.ansi().fg(Ansi.Color.WHITE).bold().toString());
        replacements.put(SRChatColor.MAGIC, Ansi.ansi().a(Attribute.BLINK_SLOW).toString());
        replacements.put(SRChatColor.BOLD, Ansi.ansi().a(Attribute.UNDERLINE_DOUBLE).toString());
        replacements.put(SRChatColor.STRIKETHROUGH, Ansi.ansi().a(Attribute.STRIKETHROUGH_ON).toString());
        replacements.put(SRChatColor.UNDERLINE, Ansi.ansi().a(Attribute.UNDERLINE).toString());
        replacements.put(SRChatColor.ITALIC, Ansi.ansi().a(Attribute.ITALIC).toString());
        replacements.put(SRChatColor.RESET, Ansi.ansi().a(Attribute.RESET).fg(Ansi.Color.DEFAULT).toString());
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
