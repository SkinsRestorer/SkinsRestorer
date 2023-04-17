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
package net.skinsrestorer.shared.gui;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.storage.SkinStorage;
import net.skinsrestorer.shared.commands.library.CommandManager;
import net.skinsrestorer.shared.listeners.event.ClickEventInfo;
import net.skinsrestorer.shared.plugin.SRServerAdapter;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRForeign;
import net.skinsrestorer.shared.subjects.SRServerPlayer;

import javax.inject.Inject;
import java.util.function.Consumer;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SharedGUI {
    public static final int HEAD_COUNT_PER_PAGE = 36;
    private final SkinStorage skinStorage;

    public <T> T createGUI(GUIManager<T> manager, Consumer<ClickEventInfo> callback, SRForeign player, int page) {
        if (page > 999) {
            page = 999;
        }

        int skinNumber = HEAD_COUNT_PER_PAGE * page;

        return manager.createGUI(callback, player, page, skinStorage.getSkins(skinNumber));
    }

    @RequiredArgsConstructor(onConstructor_ = @Inject)
    public static class ServerGUIActions implements Consumer<ClickEventInfo> {
        private final SRServerAdapter<?> adapter;
        private final CommandManager<SRCommandSender> commandManager;

        @Override
        public void accept(ClickEventInfo event) {
            SRServerPlayer player = event.getPlayer();
            switch (event.getMaterial()) {
                case HEAD:
                    commandManager.executeCommand(player, "skin set " + event.getDisplayName());
                    player.closeInventory();
                    break;
                case RED_PANE:
                    commandManager.executeCommand(player, "skin clear");
                    player.closeInventory();
                    break;
                case GREEN_PANE:
                    adapter.runAsync(() -> adapter.openServerGUI(player, event.getCurrentPage() + 1));
                    break;
                case YELLOW_PANE:
                    adapter.runAsync(() -> adapter.openServerGUI(player, event.getCurrentPage() - 1));
                    break;
                default:
                    break;
            }
        }
    }

    @RequiredArgsConstructor(onConstructor_ = @Inject)
    public static class ProxyGUIActions implements Consumer<ClickEventInfo> {
        private final SRServerAdapter<?> adapter;

        @Override
        public void accept(ClickEventInfo event) {
            SRServerPlayer player = event.getPlayer();
            switch (event.getMaterial()) {
                case HEAD:
                    String skinName = event.getDisplayName();
                    adapter.runAsync(() -> adapter.sendToMessageChannel(event.getPlayer(), out -> {
                        out.writeUTF("setSkin");
                        out.writeUTF(player.getName());
                        out.writeUTF(skinName);
                    }));
                    player.closeInventory();
                    break;
                case RED_PANE:
                    adapter.runAsync(() -> adapter.sendToMessageChannel(event.getPlayer(), out -> {
                        out.writeUTF("clearSkin");
                        out.writeUTF(player.getName());
                    }));
                    player.closeInventory();
                    break;
                case GREEN_PANE:
                    adapter.runAsync(() ->
                            adapter.requestSkinsFromProxy(event.getPlayer(), event.getCurrentPage() + 1));
                    break;
                case YELLOW_PANE:
                    adapter.runAsync(() ->
                            adapter.requestSkinsFromProxy(event.getPlayer(), event.getCurrentPage() - 1));
                    break;
                default:
                    break;
            }
        }
    }
}
