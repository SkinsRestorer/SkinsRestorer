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
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.interfaces.SRCommandSender;
import net.skinsrestorer.shared.interfaces.SRProxyPlayer;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class WrapperBungee {
    private final SettingsManager settings;
    private final SkinsRestorerLocale locale;

    public SRCommandSender commandSender(CommandSender sender) {
        if (sender instanceof ProxiedPlayer) {
            return player((ProxiedPlayer) sender);
        }

        return WrapperCommandSender.builder().sender(sender).locale(locale).settings(settings).build();
    }

    public SRProxyPlayer player(ProxiedPlayer player) {
        return WrapperPlayer.builder().player(player).sender(player).locale(locale).settings(settings).build();
    }
}
