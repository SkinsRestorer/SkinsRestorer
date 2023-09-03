/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
package net.skinsrestorer.sponge.listeners;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.utils.Tristate;
import net.skinsrestorer.sponge.SRSpongeAdapter;
import net.skinsrestorer.sponge.wrapper.WrapperSponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

import javax.inject.Inject;

/**
 * Allow players nicely to enable metrics, while also complying with Sponge regulations.
 */
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MetricsJoinListener implements EventListener<ServerSideConnectionEvent.Join> {
    private final SRSpongeAdapter adapter;
    private final SkinsRestorerLocale locale;
    private final WrapperSponge wrapper;
    private final GsonComponentSerializer gsonSerializer = GsonComponentSerializer.gson();

    @Override
    public void handle(ServerSideConnectionEvent.Join event) {
        ServerPlayer player = event.player();
        SRPlayer srPlayer = wrapper.player(player);

        if (player.hasPermission("sponge.command.metrics") && adapter.getMetricsState() == Tristate.UNDEFINED) {
            Component component = gsonSerializer.deserialize(locale.getMessage(srPlayer, Message.SPONGE_METRICS_CONSENT));
            Component hoverComponent = gsonSerializer.deserialize(locale.getMessage(srPlayer, Message.SPONGE_METRICS_HOVER));
            component = component.clickEvent(ClickEvent.runCommand("/srmetricsenable"));
            component = component.hoverEvent(HoverEvent.showText(hoverComponent));

            player.sendMessage(component);
        }
    }
}
