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
package net.skinsrestorer.modded.fabric;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.permissions.Permission;
import net.skinsrestorer.shared.utils.Tristate;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.fabric.FabricServerCommandManager;

@SuppressWarnings("unused")
public class SRModPlatformImpl {
    public static CommandManager<SRCommandSender> createCommandManager(ExecutionCoordinator<SRCommandSender> executionCoordinator,
                                                                       final SenderMapper<CommandSourceStack, SRCommandSender> senderMapper) {
        return new FabricServerCommandManager<>(executionCoordinator, senderMapper);
    }

    public static Tristate test(CommandSourceStack stack, Permission permission) {
        return switch (Permissions.getPermissionValue(stack, permission.getPermissionString())) {
            case TRUE -> Tristate.TRUE;
            case FALSE -> Tristate.FALSE;
            case DEFAULT -> Tristate.UNDEFINED;
        };
    }

    public static void registerPermission(Permission permission, Component description) {
        // NO-OP
    }
}
