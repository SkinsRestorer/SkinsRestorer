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
package net.skinsrestorer.shared.gui;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.commands.library.SRCommandManager;
import net.skinsrestorer.shared.listeners.event.ClickEventInfo;
import net.skinsrestorer.shared.plugin.SRServerAdapter;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.subjects.SRServerPlayer;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SharedGUI {
    public static final int HEAD_COUNT_PER_PAGE = 9 * 4;
    public static final String SR_PROPERTY_INTERNAL_NAME = "skinsrestorer.skull-internal-name";

    @RequiredArgsConstructor(onConstructor_ = @Inject)
    public static class ServerGUIClickCallback implements ClickEventHandler {
        private final SRServerAdapter adapter;
        private final SkinStorageImpl skinStorage;
        private final SRCommandManager commandManager;

        @Override
        public void handle(ClickEventInfo event) {
            SRServerPlayer player = event.player();
            switch (event.material()) {
                case HEAD -> {
                    commandManager.execute(player, "skin set " + event.skinName());
                    player.closeInventory();
                }
                case RED_PANE -> {
                    commandManager.execute(player, "skin clear");
                    player.closeInventory();
                }
                case GREEN_PANE ->
                        adapter.runAsync(() -> adapter.openGUIPage(player, skinStorage.getGUIPage(player, event.pageInfo().page() + 1, event.pageInfo().pageType())));
                case YELLOW_PANE ->
                        adapter.runAsync(() -> adapter.openGUIPage(player, skinStorage.getGUIPage(player, event.pageInfo().page() - 1, event.pageInfo().pageType())));
            }
        }
    }

    @RequiredArgsConstructor(onConstructor_ = @Inject)
    public static class ProxyGUIClickCallback implements ClickEventHandler {
        private final SRServerAdapter adapter;

        @Override
        public void handle(ClickEventInfo event) {
            SRServerPlayer player = event.player();
            switch (event.material()) {
                case HEAD -> {
                    adapter.runAsync(() -> event.player().sendToMessageChannel(out -> {
                        out.writeUTF("setSkinV2");
                        out.writeUTF(event.skinName());
                    }));
                    player.closeInventory();
                }
                case RED_PANE -> {
                    adapter.runAsync(() -> event.player().sendToMessageChannel(out ->
                            out.writeUTF("clearSkinV2")));
                    player.closeInventory();
                }
                case GREEN_PANE ->
                        adapter.runAsync(() -> event.player().requestSkinsFromProxy(event.pageInfo().page() + 1, event.pageInfo().pageType()));
                case YELLOW_PANE ->
                        adapter.runAsync(() -> event.player().requestSkinsFromProxy(event.pageInfo().page() - 1, event.pageInfo().pageType()));
            }
        }
    }
}
