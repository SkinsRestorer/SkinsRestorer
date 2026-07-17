/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
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
package net.skinsrestorer.fand.wrapper;

import io.fand.api.command.CommandSender;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import net.skinsrestorer.shared.subjects.AbstractSRCommandSender;
import net.skinsrestorer.shared.subjects.messages.ComponentString;
import net.skinsrestorer.shared.subjects.permissions.Permission;
import net.skinsrestorer.shared.utils.Tristate;

@SuperBuilder
public class WrapperCommandSender extends AbstractSRCommandSender {
    protected final @NonNull CommandSender sender;

    @Override
    public <S> S getAs(Class<S> senderClass) {
        return senderClass.cast(sender);
    }

    @Override
    public void sendMessage(ComponentString messageJson) {
        sender.sendMessage(FandComponentHelper.deserialize(messageJson));
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return permission.checkPermission(node -> Tristate.fromBoolean(sender.can(node)));
    }
}
