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
package net.skinsrestorer.shared.commands;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.commands.library.CommandHelpService;
import net.skinsrestorer.shared.commands.library.SRCommandService;
import net.skinsrestorer.shared.commands.library.annotations.CommandDescription;
import net.skinsrestorer.shared.commands.library.annotations.CommandPermission;
import net.skinsrestorer.shared.commands.library.annotations.RootDescription;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;

import javax.inject.Inject;
import java.util.List;

/**
 * Minimal SR command for proxy mode - only provides dump and status commands.
 */
@Command("sr|skinsrestorer")
@RootDescription(Message.HELP_SR)
@SuppressWarnings("unused")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class SRProxyCommand {
    private final CommandHelpService helpService;
    private final SRCommandService srCommandService;

    @Command("")
    @CommandPermission(PermissionRegistry.SR)
    public void rootCommand(SRCommandSender sender) {
        helpService.sendRootHelp(sender, "sr");
    }

    @Suggestions("help_queries_sr")
    public List<String> suggestHelpQueries(CommandContext<SRCommandSender> ctx, String input) {
        return helpService.suggestHelpQueries(ctx.sender(), "sr");
    }

    @Command("help [query]")
    @CommandPermission(PermissionRegistry.SR)
    @CommandDescription(Message.HELP_SR)
    public void commandHelp(SRCommandSender sender, @Argument(suggestions = "help_queries_sr") @Greedy String query) {
        helpService.sendQueryHelp(sender, "sr", query);
    }

    @Command("status")
    @CommandPermission(PermissionRegistry.SR_STATUS)
    @CommandDescription(Message.HELP_SR_STATUS)
    private void onStatus(SRCommandSender sender) {
        srCommandService.executeStatus(sender);
    }

    @Command("dump")
    @CommandPermission(PermissionRegistry.SR_DUMP)
    @CommandDescription(Message.HELP_SR_DUMP)
    private void onDump(SRCommandSender sender) {
        srCommandService.executeDump(sender);
    }
}
