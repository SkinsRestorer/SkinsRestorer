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
package net.skinsrestorer.shared.commands;

import ch.jalu.injector.Injector;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.commands.library.annotations.CommandDescription;
import net.skinsrestorer.shared.commands.library.annotations.CommandPermission;
import net.skinsrestorer.shared.commands.library.annotations.RootDescription;
import net.skinsrestorer.shared.gui.PageType;
import net.skinsrestorer.shared.gui.SharedGUI;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.storage.GUIStorage;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;
import net.skinsrestorer.shared.utils.MetricsCounter;
import org.incendo.cloud.annotations.Command;

import javax.inject.Inject;

@SuppressWarnings("unused")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class GUICommand {
    private final SRPlatformAdapter platformAdapter;
    private final GUIStorage guiStorage;
    private final SharedGUI sharedGUI;
    private final Injector injector;
    private final MetricsCounter metricsCounter;

    @Command("skins")
    @RootDescription(Message.HELP_SKINS)
    @CommandDescription(Message.HELP_SKINS)
    @CommandPermission(value = PermissionRegistry.SKINS)
    private void onDefault(SRPlayer player) {
        player.sendMessage(Message.SKINSMENU_OPEN);
        platformAdapter.openGUI(player, sharedGUI.createGUIPage(player, guiStorage.getGUIPage(player, 0, PageType.SELECT)));
        metricsCounter.increment(MetricsCounter.CommandType.SKIN_GUI);
    }
}
