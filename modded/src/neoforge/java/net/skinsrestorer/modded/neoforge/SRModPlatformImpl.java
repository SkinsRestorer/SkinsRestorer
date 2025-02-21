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
package net.skinsrestorer.modded.neoforge;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import net.skinsrestorer.modded.SRModPlatform;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.permissions.Permission;
import net.skinsrestorer.shared.utils.Tristate;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.neoforge.NeoForgeServerCommandManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unused")
public class SRModPlatformImpl implements SRModPlatform {
    private static final Map<String, PermissionNode<Boolean>> PERMISSIONS = new HashMap<>();

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public CommandManager<SRCommandSender> createCommandManager(ExecutionCoordinator<SRCommandSender> executionCoordinator,
                                                                SenderMapper<CommandSourceStack, SRCommandSender> senderMapper) {
        return new NeoForgeServerCommandManager<>(executionCoordinator, senderMapper);
    }

    @Override
    public Tristate test(CommandSourceStack stack, Permission permission) {
        if (!stack.isPlayer()) {
            return stack.hasPermission(stack.getServer().getOperatorUserPermissionLevel()) ? Tristate.TRUE : Tristate.UNDEFINED;
        }

        return PermissionAPI.getPermission(Objects.requireNonNull(stack.getPlayer()), PERMISSIONS.get(permission.getPermissionString())) ? Tristate.TRUE : Tristate.FALSE;
    }

    @Override
    public void registerPermission(Permission permission, Component description) {
        int dotIndex = permission.getPermissionString().indexOf('.');
        String beforeDot = permission.getPermissionString().substring(0, dotIndex);
        String afterDot = permission.getPermissionString().substring(dotIndex + 1);
        PermissionNode<Boolean> node = new PermissionNode<>(beforeDot, afterDot, PermissionTypes.BOOLEAN, (arg, uUID, permissionDynamicContexts) -> permission.isInDefaultGroup());
        node.setInformation(Component.literal(permission.getPermissionString()), description);

        PERMISSIONS.put(permission.getPermissionString(), node);
        NeoForge.EVENT_BUS.addListener(PermissionGatherEvent.Nodes.class, event -> event.addNodes(node));
    }
}
