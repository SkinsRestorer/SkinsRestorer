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
package net.skinsrestorer.mod.fabric;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.skinsrestorer.mod.SRModPlatform;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.permissions.Permission;
import net.skinsrestorer.shared.utils.Tristate;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.fabric.FabricServerCommandManager;

@SuppressWarnings("unused")
public class SRModPlatformImpl implements SRModPlatform {
    private static final boolean HAS_PERMISSIONS_API = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public CommandManager<SRCommandSender> createCommandManager(ExecutionCoordinator<SRCommandSender> executionCoordinator,
                                                                SenderMapper<CommandSourceStack, SRCommandSender> senderMapper) {
        return new FabricServerCommandManager<>(executionCoordinator, senderMapper);
    }

    @Override
    public Tristate test(CommandSourceStack stack, Permission permission) {
        if (HAS_PERMISSIONS_API) {
            return switch (Permissions.getPermissionValue(stack, permission.getPermissionString())) {
                case TRUE -> Tristate.TRUE;
                case FALSE -> Tristate.FALSE;
                case DEFAULT -> Tristate.UNDEFINED;
            };
        }

        return stack.permissions().hasPermission(net.minecraft.server.permissions.Permission.Atom.create(permission.getPermissionString()))
                ? Tristate.TRUE : Tristate.UNDEFINED;
    }

    @Override
    public void registerPermission(Permission permission, Component description) {
        // NO-OP
    }
}
