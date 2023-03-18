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
package net.skinsrestorer.shared.plugin;

import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.utils.IOExceptionConsumer;

import java.io.DataOutputStream;
import java.util.Map;
import java.util.Optional;

public interface SRServerAdapter<P> extends SRPlatformAdapter<P> {
    void runSync(Runnable runnable);

    boolean determineProxy();

    void openServerGUI(SRPlayer player, int page);

    void openProxyGUI(SRPlayer player, int page, Map<String, String> skinList);

    Optional<SRPlayer> getPlayer(String name);

    default void requestSkinsFromProxy(SRPlayer player, int page) {
        sendToMessageChannel(player, out -> {
            out.writeUTF("getSkins");
            out.writeUTF(player.getName());
            out.writeInt(page);
        });
    }

    void sendToMessageChannel(SRPlayer player, IOExceptionConsumer<DataOutputStream> consumer);
}
