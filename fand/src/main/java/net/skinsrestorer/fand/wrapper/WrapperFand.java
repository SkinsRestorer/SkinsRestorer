/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
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
package net.skinsrestorer.fand.wrapper;

import ch.jalu.configme.SettingsManager;
import io.fand.api.command.CommandSender;
import io.fand.api.entity.Player;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.fand.SRFandAdapter;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRServerPlayer;
import net.skinsrestorer.shared.subjects.SRSubjectWrapper;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class WrapperFand implements SRSubjectWrapper<CommandSender, Player, SRServerPlayer> {
    private final SettingsManager settings;
    private final SkinsRestorerLocale locale;
    private final SRFandAdapter adapter;

    @Override
    public SRCommandSender commandSender(CommandSender sender) {
        if (sender instanceof Player player) {
            return player(player);
        }
        return WrapperCommandSender.builder()
                .sender(sender)
                .settings(settings)
                .locale(locale)
                .build();
    }

    @Override
    public SRServerPlayer player(Player player) {
        return WrapperPlayer.builder()
                .player(player)
                .sender(player)
                .adapter(adapter)
                .settings(settings)
                .locale(locale)
                .build();
    }

    @Override
    public CommandSender unwrap(SRCommandSender sender) {
        return sender.getAs(CommandSender.class);
    }
}
