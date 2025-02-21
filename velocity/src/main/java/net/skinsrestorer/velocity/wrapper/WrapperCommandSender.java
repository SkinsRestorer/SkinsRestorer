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

import com.velocitypowered.api.command.CommandSource;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import net.skinsrestorer.shared.subjects.AbstractSRCommandSender;
import net.skinsrestorer.shared.subjects.messages.ComponentString;
import net.skinsrestorer.shared.subjects.permissions.Permission;
import net.skinsrestorer.shared.utils.Tristate;

@SuperBuilder
public class WrapperCommandSender extends AbstractSRCommandSender {
    private final @NonNull CommandSource sender;

    @Override
    public <S> S getAs(Class<S> senderClass) {
        return senderClass.cast(sender);
    }

    @Override
    public void sendMessage(ComponentString messageJson) {
        sender.sendMessage(VelocityComponentHelper.deserialize(messageJson));
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return permission.checkPermission(p -> switch (sender.getPermissionValue(p)) {
            case TRUE -> Tristate.TRUE;
            case FALSE -> Tristate.FALSE;
            case UNDEFINED -> Tristate.UNDEFINED;
        });
    }
}
