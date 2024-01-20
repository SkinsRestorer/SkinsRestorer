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
package net.skinsrestorer.sponge.wrapper;

import ch.jalu.configme.SettingsManager;
import lombok.experimental.SuperBuilder;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.skinsrestorer.shared.subjects.AbstractSRCommandSender;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.subjects.permissions.Permission;
import net.skinsrestorer.shared.subjects.permissions.PermissionGroup;
import net.skinsrestorer.shared.utils.Tristate;
import org.spongepowered.api.service.permission.Subject;

@SuperBuilder
public class WrapperCommandSender extends AbstractSRCommandSender {
    private final SettingsManager settings;
    private final SkinsRestorerLocale locale;
    private final Subject subject;
    private final Audience audience;
    private final GsonComponentSerializer serializer = GsonComponentSerializer.gson();

    @Override
    public void sendMessage(String messageJson) {
        audience.sendMessage(serializer.deserialize(messageJson));
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return permission.checkPermission(settings, p -> switch (subject.permissionValue(p)) {
            case TRUE -> Tristate.TRUE;
            case FALSE -> Tristate.FALSE;
            case UNDEFINED -> Tristate.fromBoolean(
                    PermissionGroup.DEFAULT_GROUP.hasPermission(permission)
            );
        });
    }

    @Override
    protected SkinsRestorerLocale getSRLocale() {
        return locale;
    }

    @Override
    protected SettingsManager getSettings() {
        return settings;
    }
}
