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
package net.skinsrestorer.fand.command;

import io.fand.api.command.CommandRegistry;
import io.fand.api.command.CommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.SenderMapperHolder;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.slf4j.Logger;

public final class FandCommandManager<C> extends CommandManager<C>
        implements SenderMapperHolder<CommandSender, C> {
    private final SenderMapper<CommandSender, C> senderMapper;

    @SuppressWarnings("this-escape")
    public FandCommandManager(
            CommandRegistry commandRegistry,
            ExecutionCoordinator<C> executionCoordinator,
            SenderMapper<CommandSender, C> senderMapper,
            Logger logger
    ) {
        super(executionCoordinator, new FandCommandRegistrationHandler<>(commandRegistry));
        this.senderMapper = senderMapper;
        ((FandCommandRegistrationHandler<C>) commandRegistrationHandler()).initialize(this);
        parameterInjectorRegistry().registerInjector(
                CommandSender.class,
                (context, annotations) -> senderMapper.reverse(context.sender()));
        registerDefaultExceptionHandlers(
                triplet -> senderMapper.reverse(triplet.first().sender()).sendMessage(Component.text(
                        triplet.first().formatCaption(triplet.second(), triplet.third()),
                        NamedTextColor.RED)),
                pair -> logger.error("Failed to execute SkinsRestorer command", pair.second()));
    }

    @Override
    public boolean hasPermission(C sender, String permission) {
        return senderMapper.reverse(sender).can(permission);
    }

    @Override
    public SenderMapper<CommandSender, C> senderMapper() {
        return senderMapper;
    }
}
