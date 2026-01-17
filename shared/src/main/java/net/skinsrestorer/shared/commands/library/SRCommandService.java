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

import ch.jalu.configme.SettingsManager;
import ch.jalu.injector.Injector;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.builddata.BuildData;
import net.skinsrestorer.shared.config.DevConfig;
import net.skinsrestorer.shared.connections.DumpService;
import net.skinsrestorer.shared.connections.ServiceCheckerService;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRServerPlugin;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.messages.ComponentHelper;
import net.skinsrestorer.shared.subjects.messages.Message;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;

/**
 * Shared command logic for /sr status and /sr dump commands.
 * Used by both SRCommand (full mode) and SRProxyCommand (proxy mode).
 */
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SRCommandService {
    private final SRPlatformAdapter adapter;
    private final ServiceCheckerService serviceCheckerService;
    private final SettingsManager settings;
    private final SRLogger logger;
    private final DumpService dumpService;
    private final Injector injector;

    public void executeStatus(SRCommandSender sender) {
        sender.sendMessage(Message.ADMINCOMMAND_STATUS_CHECKING);

        sender.sendMessage(Message.DIVIDER);

        ServiceCheckerService.ServiceCheckResponse response = serviceCheckerService.checkServices();
        for (ServiceCheckerService.ServiceCheckResponse.ServiceCheckMessage message : response.getResults()) {
            if (!message.success() || settings.getProperty(DevConfig.DEBUG)) {
                sender.sendMessage(ComponentHelper.parseMiniMessageToJsonString(message.message()));
            }
        }

        sender.sendMessage(Message.ADMINCOMMAND_STATUS_UUID_API,
                Placeholder.parsed("count", String.valueOf(response.getSuccessCount(ServiceCheckerService.ServiceCheckResponse.ServiceCheckType.UUID))),
                Placeholder.parsed("total", String.valueOf(response.getTotalCount(ServiceCheckerService.ServiceCheckResponse.ServiceCheckType.UUID)))
        );
        sender.sendMessage(Message.ADMINCOMMAND_STATUS_PROFILE_API,
                Placeholder.parsed("count", String.valueOf(response.getSuccessCount(ServiceCheckerService.ServiceCheckResponse.ServiceCheckType.PROFILE))),
                Placeholder.parsed("total", String.valueOf(response.getTotalCount(ServiceCheckerService.ServiceCheckResponse.ServiceCheckType.PROFILE)))
        );

        if (response.allFullySuccessful()) {
            sender.sendMessage(Message.ADMINCOMMAND_STATUS_WORKING);
        } else if (response.minOneServiceUnavailable()) {
            sender.sendMessage(Message.ADMINCOMMAND_STATUS_BROKEN);
            sender.sendMessage(Message.ADMINCOMMAND_STATUS_FIREWALL);
        } else {
            sender.sendMessage(Message.ADMINCOMMAND_STATUS_DEGRADED);
        }

        sender.sendMessage(Message.DIVIDER);
        sender.sendMessage(Message.ADMINCOMMAND_STATUS_SUMMARY_VERSION, Placeholder.parsed("version", BuildData.VERSION));
        sender.sendMessage(Message.ADMINCOMMAND_STATUS_SUMMARY_SERVER, Placeholder.parsed("version", adapter.getPlatformVersion()));

        SRServerPlugin serverPlugin = injector.getIfAvailable(SRServerPlugin.class);
        if (serverPlugin != null) {
            sender.sendMessage(Message.ADMINCOMMAND_STATUS_SUMMARY_PROXYMODE, Placeholder.parsed("proxy_mode", Boolean.toString(serverPlugin.isProxyMode())));
        }

        sender.sendMessage(Message.ADMINCOMMAND_STATUS_SUMMARY_COMMIT, Placeholder.parsed("hash", BuildData.COMMIT_SHORT));
        sender.sendMessage(Message.DIVIDER);
    }

    public void executeDump(SRCommandSender sender) {
        try {
            sender.sendMessage(Message.ADMINCOMMAND_DUMP_UPLOADING);
            Optional<String> url = dumpService.dump();
            if (url.isPresent()) {
                sender.sendMessage(Message.ADMINCOMMAND_DUMP_SUCCESS, Placeholder.parsed("url", "https://bytebin.lucko.me/%s".formatted(url.get())));
            } else {
                sender.sendMessage(Message.ADMINCOMMAND_DUMP_ERROR);
            }
        } catch (IOException | DataRequestException e) {
            logger.severe("Failed to dump data", e);
            sender.sendMessage(Message.ADMINCOMMAND_DUMP_ERROR);
        }
    }
}
