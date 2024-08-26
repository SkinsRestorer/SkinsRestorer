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
package net.skinsrestorer.bukkit.wrapper;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import net.kyori.adventure.text.Component;
import net.skinsrestorer.bukkit.SRBukkitAdapter;
import net.skinsrestorer.shared.subjects.AbstractSRCommandSender;
import net.skinsrestorer.shared.subjects.messages.ComponentString;
import net.skinsrestorer.shared.subjects.permissions.Permission;
import net.skinsrestorer.shared.utils.Tristate;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;

@SuperBuilder
public class WrapperCommandSender extends AbstractSRCommandSender {
    protected final @NonNull SRBukkitAdapter adapter;
    private final @NonNull CommandSender sender;

    @Override
    public <S> S getAs(Class<S> senderClass) {
        return senderClass.cast(sender);
    }

    @Override
    public void sendMessage(ComponentString messageJson) {
        Component message = BukkitComponentHelper.deserialize(messageJson);

        Runnable runnable = () -> adapter.getAdventure().get().sender(sender).sendMessage(message);
        if (sender instanceof BlockCommandSender) {
            // Command blocks require messages to be sent synchronously in Bukkit
            adapter.runSync(this, runnable);
        } else {
            runnable.run();
        }
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return permission.checkPermission(p -> Tristate.fromBoolean(sender.hasPermission(p)));
    }
}
