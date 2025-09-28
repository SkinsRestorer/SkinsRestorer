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

import lombok.RequiredArgsConstructor;
import net.lenni0451.reflect.exceptions.ConstructorNotFoundException;
import net.lenni0451.reflect.stream.RStream;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.bungee.wrapper.WrapperBungee;
import net.skinsrestorer.shared.api.SkinApplierAccess;
import net.skinsrestorer.shared.api.event.EventBusImpl;
import net.skinsrestorer.shared.api.event.SkinApplyEventImpl;
import net.skinsrestorer.shared.codec.SRServerPluginMessage;
import net.skinsrestorer.shared.subjects.SRProxyPlayer;
import net.skinsrestorer.shared.utils.AuthLibHelper;
import net.skinsrestorer.shared.utils.ProxyAckTracker;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Optional;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinApplierBungee implements SkinApplierAccess<ProxiedPlayer> {
    private final WrapperBungee wrapper;
    private final EventBusImpl eventBus;
    private final ProxyAckTracker proxyAckTracker;

    public static void applyToHandler(PendingConnection handler, SkinProperty property) {
        // LoginResult wrapper
        var loginProfileFieldWrapper = RStream.of(handler)
                .withSuper() // Include custom implementations that extend InitialHandler
                .fields()
                .by("loginProfile");
        // LoginResult.class
        var loginProfileClass = loginProfileFieldWrapper.type();
        // LoginProfile instance
        var loginProfile = loginProfileFieldWrapper.get();
        // Property[] wrapper
        var propertyArrayFieldWrapper = RStream.of(loginProfileFieldWrapper.type())
                .fields()
                .by("properties");
        // Property[].class
        var propertyArrayClass = propertyArrayFieldWrapper.type();
        // Property.class
        var propertyClass = propertyArrayClass.getComponentType();
        // Our new Property[] instance
        var propertyArray = Array.newInstance(propertyClass, 1);
        Array.set(propertyArray, 0, RStream.of(propertyClass)
                .constructors()
                .by(String.class, String.class, String.class)
                .newInstance(SkinProperty.TEXTURES_NAME, property.getValue(), property.getSignature()));

        if (loginProfile == null) {
            try {
                // New BungeeCord
                // new LoginResult(String, String, Property[])
                loginProfileFieldWrapper.set(RStream.of(loginProfileClass)
                        .constructors()
                        .by(String.class, String.class, propertyArrayClass)
                        .newInstance(null, null, propertyArray));
            } catch (ConstructorNotFoundException ignored) {
                // Old BungeeCord
                // new LoginResult(String, Property[])
                loginProfileFieldWrapper.set(RStream.of(loginProfileClass)
                        .constructors()
                        .by(String.class, propertyArrayClass)
                        .newInstance(null, propertyArray));
            }
        } else {
            propertyArrayFieldWrapper.set(loginProfile, propertyArray);
        }
    }

    public static Optional<SkinProperty> getSkinProperty(ProxiedPlayer player) {
        var properties = (Object[]) RStream.of(player.getPendingConnection())
                .withSuper() // Include custom implementations that extend InitialHandler
                .fields()
                .by("loginProfile")
                .stream()
                .fields()
                .by("properties")
                .get();

        if (properties == null) {
            return Optional.empty();
        }

        return Arrays.stream(properties)
                .map(property -> SkinProperty.tryParse(
                        AuthLibHelper.getPropertyName(property),
                        AuthLibHelper.getPropertyValue(property),
                        AuthLibHelper.getPropertySignature(property)
                ))
                .flatMap(Optional::stream)
                .findFirst();
    }

    @Override
    public void applySkin(ProxiedPlayer player, SkinProperty property) {
        applyEvent(player, property, player.getPendingConnection());
    }

    public void applySkin(SkinProperty property, PendingConnection handler) {
        applyEvent(null, property, handler);
    }

    private void applyEvent(@Nullable ProxiedPlayer player, SkinProperty property, PendingConnection handler) {
        SkinApplyEventImpl event = new SkinApplyEventImpl(player, property);

        eventBus.callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        applyWithProperty(player, handler, event.getProperty());
    }

    private void applyWithProperty(@Nullable ProxiedPlayer player, PendingConnection handler, SkinProperty property) {
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
