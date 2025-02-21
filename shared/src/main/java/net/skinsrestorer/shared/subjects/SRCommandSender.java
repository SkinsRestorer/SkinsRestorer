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
package net.skinsrestorer.shared.subjects;

import net.skinsrestorer.shadow.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.skinsrestorer.shared.subjects.messages.ComponentString;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.permissions.Permission;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;

public interface SRCommandSender extends SRForeign {
    <S> S getAs(Class<S> senderClass);

    void sendMessage(ComponentString messageJson);

    void sendMessage(Message key, TagResolver... resolvers);

    boolean hasPermission(Permission permission);

    default boolean hasPermission(PermissionRegistry registry) {
        return hasPermission(registry.getPermission());
    }
}
