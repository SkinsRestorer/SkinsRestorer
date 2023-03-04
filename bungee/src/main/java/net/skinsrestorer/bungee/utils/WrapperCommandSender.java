/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
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
package net.skinsrestorer.bungee.utils;

import ch.jalu.configme.SettingsManager;
import lombok.experimental.SuperBuilder;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.config.MessageConfig;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.subjects.SRCommandSender;

import java.util.Locale;

@SuperBuilder
public class WrapperCommandSender implements SRCommandSender {
    private final SettingsManager settings;
    private final SkinsRestorerLocale locale;
    private final CommandSender sender;

    @Override
    public Locale getLocale() {
        return settings.getProperty(MessageConfig.LANGUAGE);
    }

    @Override
    public void sendMessage(String message) {
        sender.sendMessage(TextComponent.fromLegacyText(message));
    }

    @Override
    public void sendMessage(Message key, Object... args) {
        sendMessage(locale.getMessage(this, key, args));
    }

    @Override
    public String getName() {
        return sender.getName();
    }

    @Override
    public boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }
}
