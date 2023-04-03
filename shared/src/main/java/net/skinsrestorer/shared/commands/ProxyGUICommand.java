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
import net.skinsrestorer.shared.commands.library.annotations.*;
import net.skinsrestorer.shared.plugin.SRProxyPlugin;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.subjects.PermissionRegistry;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.SRProxyPlayer;

import javax.inject.Inject;

@SuppressWarnings("unused")
@PublicVisibility
@CommandNames("skins")
@CommandPermission(value = PermissionRegistry.SKINS)
@CommandConditions({"cooldown", "allowed-server"})
@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class ProxyGUICommand {
    private final SkinStorageImpl skinStorage;
    private final SRProxyPlugin proxyPlugin;

    @RootCommand
    private void onDefault(SRPlayer player) {
        if (!(player instanceof SRProxyPlayer)) {
            throw new IllegalStateException("Player is not a proxy player");
        }

        player.sendMessage(Message.SKINSMENU_OPEN);

        proxyPlugin.sendPage(0, (SRProxyPlayer) player, skinStorage);
    }
}
