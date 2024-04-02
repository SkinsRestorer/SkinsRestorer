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
package net.skinsrestorer.shared.plugin;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.subjects.SRProxyPlayer;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SRProxyPlugin {
    private final SRPlugin plugin;

    public void sendPage(int page, SRProxyPlayer player, SkinStorageImpl skinStorage) {
        int skinOffset = 36 * page;

        player.sendPage(page, skinStorage.getGUISkins(skinOffset));
    }

    public void startupPlatform(SRProxyPlatformInit init) throws InitializeException {
        // Init storage
        plugin.loadStorage();

        // Init API
        plugin.registerAPI();

        // Init listener
        init.initLoginProfileListener();
        init.initConnectListener();

        // Init commands
        plugin.initCommands();

        init.initMessageChannel();
    }
}
