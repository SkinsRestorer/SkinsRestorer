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
package net.skinsrestorer.shared.commands;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.commands.library.annotations.CommandConditions;
import net.skinsrestorer.shared.commands.library.annotations.CommandNames;
import net.skinsrestorer.shared.commands.library.annotations.CommandPermission;
import net.skinsrestorer.shared.commands.library.annotations.Default;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRServerAdapter;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.subjects.PermissionRegistry;
import net.skinsrestorer.shared.subjects.SRPlayer;

import javax.inject.Inject;

@SuppressWarnings({"unused"})
@CommandNames("skins")
@CommandPermission(PermissionRegistry.SKINS)
@CommandConditions("cooldown")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ServerGUICommand {
    private final SRServerAdapter<?> plugin;
    private final SkinsRestorerLocale locale;
    private final SRLogger logger;
    private final SkinStorageImpl skinStorage;

    @Default
    private void onDefault(SRPlayer srPlayer) {
        plugin.runAsync(() -> {
            srPlayer.sendMessage(Message.SKINSMENU_OPEN);

            plugin.openServerGUI(srPlayer, 0);
        });
    }
}
