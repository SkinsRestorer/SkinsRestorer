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
package net.skinsrestorer.bungee.wrapper;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import net.md_5.bungee.api.CommandSender;
import net.skinsrestorer.bungee.SRBungeeAdapter;
import net.skinsrestorer.shared.config.CommandConfig;
import net.skinsrestorer.shared.subjects.AbstractSRCommandSender;
import net.skinsrestorer.shared.subjects.messages.ComponentString;
import net.skinsrestorer.shared.subjects.permissions.Permission;
import net.skinsrestorer.shared.utils.Tristate;

@SuperBuilder
public class WrapperCommandSender extends AbstractSRCommandSender {
    private final @NonNull CommandSender sender;
    private final @NonNull SRBungeeAdapter adapter;

    @Override
    public <S> S getAs(Class<S> senderClass) {
        return senderClass.cast(sender);
    }

    @Override
    public void sendMessage(ComponentString messageJson) {
        adapter.getAdventure().get().sender(sender).sendMessage(BungeeComponentHelper.deserialize(messageJson));
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return permission.checkPermission(p -> {
            if (sender.hasPermission(p)) {
                return Tristate.TRUE;
            } else {
                // Since BungeeCord has no default permissions, we allow forcing a FALSE to be UNDEFINED
                // if enabled in the config. Else a FALSE means FALSE.
                return settings.getProperty(CommandConfig.FORCE_DEFAULT_PERMISSIONS) ? Tristate.UNDEFINED : Tristate.FALSE;
            }
        });
    }
}
