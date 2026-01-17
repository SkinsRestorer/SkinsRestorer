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
package net.skinsrestorer.mod;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.permissions.Permission;
import net.skinsrestorer.shared.utils.Tristate;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.execution.ExecutionCoordinator;

import java.util.ServiceLoader;

@SuppressWarnings("unused")
public interface SRModPlatform {
    SRModPlatform INSTANCE = ServiceLoader.load(SRModPlatform.class).findFirst().orElseThrow();

    String getPlatformName();

    CommandManager<SRCommandSender> createCommandManager(ExecutionCoordinator<SRCommandSender> executionCoordinator,
                                                         SenderMapper<CommandSourceStack, SRCommandSender> senderMapper);

    Tristate test(CommandSourceStack stack, Permission permission);

    void registerPermission(Permission permission, Component description);
}
