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
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.protocol.Property;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.bungee.wrapper.WrapperBungee;
import net.skinsrestorer.shared.api.SkinApplierAccess;
import net.skinsrestorer.shared.api.event.EventBusImpl;
import net.skinsrestorer.shared.api.event.SkinApplyEventImpl;
import net.skinsrestorer.shared.codec.SRServerPluginMessage;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.subjects.SRProxyPlayer;
import net.skinsrestorer.shared.utils.ProxyAckTracker;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinApplierBungee implements SkinApplierAccess<ProxiedPlayer> {
    private final WrapperBungee wrapper;
    private final EventBusImpl eventBus;
    private final SRLogger logger;
    private final ProxyAckTracker proxyAckTracker;

    public static void applyToHandler(InitialHandler handler, SkinProperty property) {
        LoginResult profile = handler.getLoginProfile();
        Property[] newProps = new Property[]{new Property(SkinProperty.TEXTURES_NAME, property.getValue(), property.getSignature())};

        if (profile == null) {
            try {
                Field field = InitialHandler.class.getDeclaredField("loginProfile");
                field.setAccessible(true);
                field.set(handler, new LoginResult(null, null, newProps));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to apply skin property to InitialHandler", e);
            }
        } else {
            profile.setProperties(newProps);
        }
    }

    public static Optional<SkinProperty> getSkinProperty(ProxiedPlayer player) {
        Property[] props = ((InitialHandler) player.getPendingConnection()).getLoginProfile().getProperties();

        if (props == null) {
            return Optional.empty();
        }

        return Arrays.stream(props)
                .map(property -> SkinProperty.tryParse(
                        property.getName(),
                        property.getValue(),
                        property.getSignature()
                ))
                .flatMap(Optional::stream)
                .findFirst();
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
        applyToHandler(handler, property);

        if (player == null) {
            return;
        }

        SRProxyPlayer srPlayer = wrapper.player(player);
        srPlayer.sendToMessageChannel(new SRServerPluginMessage(new SRServerPluginMessage.SkinUpdateV3ChannelPayload(
                property,
                proxyAckTracker.shouldAckPayload(srPlayer)
        )));
    }
}
