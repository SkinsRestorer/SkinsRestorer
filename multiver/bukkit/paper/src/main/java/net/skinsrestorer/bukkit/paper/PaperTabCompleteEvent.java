/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.skinsrestorer.bukkit.paper;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.destroystokyo.paper.profile.ProfileProperty;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.commands.library.CommandExecutor;
import net.skinsrestorer.shared.commands.library.CommandManager;
import net.skinsrestorer.shared.commands.library.SRRegisterPayload;
import net.skinsrestorer.shared.listeners.LoginProfileListenerAdapter;
import net.skinsrestorer.shared.listeners.event.SRLoginProfileEvent;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Function;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PaperTabCompleteEvent implements Listener {
    private final SRRegisterPayload<SRCommandSender> payload;
    private final Function<CommandSender, SRCommandSender> senderMapper;

    @EventHandler(ignoreCancelled = true)
    public void onAsyncTabComplete(AsyncTabCompleteEvent event) {
        if (!event.isCommand() || event.isHandled() || event.getBuffer().isEmpty()) {
            return;
        }

        String buffer = event.getBuffer();
        if (buffer.charAt(0) == '/') {
            buffer = buffer.substring(1);
        }

        String[] args = buffer.split(" ");
        if (args.length == 0) {
            return;
        }

        String command = args[0];
        if (!command.equals(payload.getMeta().getRootName()) && !Arrays.asList(payload.getMeta().getAliases()).contains(command)) {
            return;
        }

        event.setHandled(true);
        event.setCompletions(payload.getExecutor().tabComplete(senderMapper.apply(event.getSender()), buffer).join());
    }
}
