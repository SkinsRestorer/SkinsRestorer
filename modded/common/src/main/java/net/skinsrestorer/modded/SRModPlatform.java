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
package net.skinsrestorer.modded;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.permissions.Permission;
import net.skinsrestorer.shared.utils.Tristate;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.execution.ExecutionCoordinator;

@SuppressWarnings("unused")
public class SRModPlatform {
    @ExpectPlatform
    public static CommandManager<SRCommandSender> createCommandManager(ExecutionCoordinator<SRCommandSender> executionCoordinator,
                                                                       final SenderMapper<CommandSourceStack, SRCommandSender> senderMapper) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Tristate test(CommandSourceStack stack, Permission permission) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerPermission(Permission permission, Component description) {
        throw new AssertionError();
    }
}
