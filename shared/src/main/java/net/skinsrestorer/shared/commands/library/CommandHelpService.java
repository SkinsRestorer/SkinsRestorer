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
package net.skinsrestorer.shared.commands.library;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.messages.ComponentHelper;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import org.incendo.cloud.help.result.CommandEntry;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.minecraft.extras.caption.ComponentCaptionFormatter;

import javax.inject.Inject;
import java.util.List;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CommandHelpService {
    private final SRCommandManager commandManager;
    private final SkinsRestorerLocale locale;

    public void sendRootHelp(SRCommandSender sender, String commandName) {
        MinecraftHelp.<SRCommandSender>builder()
                .commandManager(commandManager.getCommandManager())
                .audienceProvider(ComponentHelper::commandSenderToAudience)
                .commandPrefix("/%s help".formatted(commandName))
                .messageProvider(MinecraftHelp.captionMessageProvider(
                        commandManager.getCommandManager().captionRegistry(),
                        ComponentCaptionFormatter.miniMessage()
                ))
                .descriptionDecorator((s, d) -> ComponentHelper.convertJsonToComponent(locale.getMessageRequired(s, Message.fromKey(d).orElseThrow())))
                .commandFilter(c -> c.rootComponent().name().equals(commandName) && !c.commandDescription().description().isEmpty())
                .maxResultsPerPage(Integer.MAX_VALUE)
                .build()
                .queryCommands("", sender);
    }

    public void sendQueryHelp(SRCommandSender sender, String commandName, String query) {
        MinecraftHelp.<SRCommandSender>builder()
                .commandManager(commandManager.getCommandManager())
                .audienceProvider(ComponentHelper::commandSenderToAudience)
                .commandPrefix("/%s help".formatted(commandName))
                .messageProvider(MinecraftHelp.captionMessageProvider(
                        commandManager.getCommandManager().captionRegistry(),
                        ComponentCaptionFormatter.miniMessage()
                ))
                .descriptionDecorator((s, d) -> ComponentHelper.convertJsonToComponent(locale.getMessageRequired(s, Message.fromKey(d).orElseThrow())))
                .commandFilter(c -> c.rootComponent().name().equals(commandName) && !c.commandDescription().description().isEmpty())
                .build()
                .queryCommands(query == null ? "" : query, sender);
    }

    public List<String> suggestHelpQueries(SRCommandSender sender, String commandName) {
        return commandManager.getCommandManager()
                .createHelpHandler()
                .queryRootIndex(sender)
                .entries()
                .stream()
                .filter(e -> e.command().rootComponent().name().equals(commandName))
                .map(CommandEntry::syntax)
                .toList();
    }
}
