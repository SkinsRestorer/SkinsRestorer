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
package net.skinsrestorer.sponge.wrapper;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRServerPlayer;
import net.skinsrestorer.shared.subjects.SRSubjectWrapper;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.permission.Subject;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class WrapperSponge implements SRSubjectWrapper<CommandCause, ServerPlayer, SRServerPlayer> {
    private final SettingsManager settings;
    private final SkinsRestorerLocale locale;

    @Override
    public SRCommandSender commandSender(CommandCause sender) {
        Subject subject = sender.subject();
        if (subject instanceof ServerPlayer) {
            return player((ServerPlayer) subject);
        }

        return WrapperCommandSender.builder().subject(subject).audience(sender.audience()).locale(locale).settings(settings).build();
    }

    @Override
    public SRServerPlayer player(ServerPlayer player) {
        return WrapperPlayer.builder().player(player).subject(player).audience(player).locale(locale).settings(settings).build();
    }
}
