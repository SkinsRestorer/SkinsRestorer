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
package net.skinsrestorer.bukkit.wrapper;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.bukkit.SRBukkitAdapter;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRServerPlayer;
import net.skinsrestorer.shared.subjects.SRSubjectWrapper;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class WrapperBukkit implements SRSubjectWrapper<CommandSender, Player, SRServerPlayer> {
    private final SettingsManager settings;
    private final SkinsRestorerLocale locale;
    private final SRBukkitAdapter adapter;

    @Override
    public SRCommandSender commandSender(CommandSender sender) {
        if (sender instanceof Player player) {
            return player(player);
        }

        return WrapperCommandSender.builder().sender(sender).locale(locale).settings(settings).adapter(adapter).build();
    }

    @Override
    public SRServerPlayer player(Player player) {
        return WrapperPlayer.builder().player(player).sender(player).locale(locale).settings(settings).adapter(adapter).build();
    }

    @Override
    public CommandSender unwrap(SRCommandSender sender) {
        return sender.getAs(CommandSender.class);
    }
}
