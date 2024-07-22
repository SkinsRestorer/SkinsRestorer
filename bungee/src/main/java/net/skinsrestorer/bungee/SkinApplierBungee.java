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
package net.skinsrestorer.bungee;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.bungee.wrapper.WrapperBungee;
import net.skinsrestorer.shared.api.SkinApplierAccess;
import net.skinsrestorer.shared.api.event.EventBusImpl;
import net.skinsrestorer.shared.api.event.SkinApplyEventImpl;
import net.skinsrestorer.shared.codec.SRServerPluginMessage;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinApplierBungee implements SkinApplierAccess<ProxiedPlayer> {
    public static final boolean IS_NEW_PROPERTY_CLASS = ReflectionUtil.classExists("net.md_5.bungee.protocol.Property");
    @Getter
    private static final SkinApplyBungeeAdapter applyAdapter = selectSkinApplyAdapter();
    private final WrapperBungee wrapper;
    private final EventBusImpl eventBus;
    private final SRLogger logger;

    /*
     * Starting the 1.19 builds of BungeeCord, the Property class has changed.
     * This method will check if the new class is available and return the appropriate class that was compiled for it.
     */
    private static SkinApplyBungeeAdapter selectSkinApplyAdapter() {
        if (IS_NEW_PROPERTY_CLASS) {
            return new SkinApplierBungeeNew();
        } else {
            return new SkinApplierBungeeOld();
        }
    }

    @Override
    public void applySkin(ProxiedPlayer player, SkinProperty property) {
        try {
            applyEvent(player, property, (InitialHandler) player.getPendingConnection());
        } catch (ReflectiveOperationException e) {
            logger.severe("Failed to apply skin to player %s".formatted(player.getName()), e);
        }
    }

    public void applySkin(SkinProperty property, InitialHandler handler) {
        try {
            applyEvent(null, property, handler);
        } catch (ReflectiveOperationException e) {
            logger.severe("Failed to apply skin to player", e);
        }
    }

    private void applyEvent(@Nullable ProxiedPlayer player, SkinProperty property, InitialHandler handler) throws ReflectiveOperationException {
        SkinApplyEventImpl event = new SkinApplyEventImpl(player, property);

        eventBus.callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        applyWithProperty(player, handler, event.getProperty());
    }

    private void applyWithProperty(@Nullable ProxiedPlayer player, InitialHandler handler, SkinProperty property) throws ReflectiveOperationException {
        applyAdapter.applyToHandler(handler, property);

        if (player == null) {
            return;
        }

        wrapper.player(player).sendToMessageChannel(new SRServerPluginMessage(new SRServerPluginMessage.SkinUpdateChannelPayload(property)));
    }
}
