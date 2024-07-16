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
package net.skinsrestorer.velocity.wrapper;

import ch.jalu.configme.SettingsManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRProxyPlayer;
import net.skinsrestorer.shared.subjects.SRSubjectWrapper;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class WrapperVelocity implements SRSubjectWrapper<CommandSource, Player, SRProxyPlayer> {
    private final SettingsManager settings;
    private final SkinsRestorerLocale locale;

    @Override
    public SRCommandSender commandSender(CommandSource sender) {
        if (sender instanceof Player player) {
            return player(player);
        }

        return WrapperCommandSender.builder().sender(sender).locale(locale).settings(settings).build();
    }

    @Override
    public SRProxyPlayer player(Player player) {
        return WrapperPlayer.builder().player(player).sender(player).locale(locale).settings(settings).build();
    }

    @Override
    public CommandSource unwrap(SRCommandSender sender) {
        return sender.getAs(CommandSource.class);
    }
}
