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
package net.skinsrestorer.velocity.wrapper;

import ch.jalu.configme.SettingsManager;
import com.velocitypowered.api.command.CommandSource;
import lombok.experimental.SuperBuilder;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.skinsrestorer.shared.subjects.AbstractSRCommandSender;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.subjects.permissions.Permission;
import net.skinsrestorer.shared.utils.Tristate;

@SuperBuilder
public class WrapperCommandSender extends AbstractSRCommandSender {
    private final SettingsManager settings;
    private final SkinsRestorerLocale locale;
    private final CommandSource sender;
    private final GsonComponentSerializer serializer = GsonComponentSerializer.gson();

    @Override
    public void sendMessage(String messageJson) {
        sender.sendMessage(serializer.deserialize(messageJson));
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return permission.checkPermission(settings, p -> {
            return switch (sender.getPermissionValue(p)) {
                case TRUE -> Tristate.TRUE;
                case FALSE -> Tristate.FALSE;
                default -> Tristate.UNDEFINED;
            };
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
