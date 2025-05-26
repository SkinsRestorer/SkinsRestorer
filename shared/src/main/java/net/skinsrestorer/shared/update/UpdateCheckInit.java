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
package net.skinsrestorer.shared.update;

import ch.jalu.injector.Injector;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;

import javax.inject.Inject;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class UpdateCheckInit {
    private final SRPlugin plugin;
    private final SRLogger logger;
    private final SRPlatformAdapter adapter;
    private final UpdateCheckerGitHub updateChecker;
    private final UpdateCheckExecutor updateCheckExecutor;
    private final Injector injector;

    public Optional<UpdateDownloader> getDownloader() {
        boolean downloaderDisabled = Files.exists(plugin.getDataFolder().resolve("noupdate.txt"));
        if (downloaderDisabled) {
            logger.info("Auto updater was manually disabled. This is not recommended, as it will prevent the plugin from updating automatically. See why at: https://skinsrestorer.net/docs/configuration/auto-update");
        }

        DownloaderClassProvider downloaderClassProvider = injector.getIfAvailable(DownloaderClassProvider.class);
        return downloaderClassProvider == null || downloaderDisabled ? Optional.empty() : Optional.of(injector.getSingleton(downloaderClassProvider.get()));
    }

    public void run(InitCause cause) {
        UpdateDownloader downloader = getDownloader().orElse(null);
        updateCheckExecutor.checkUpdate(cause.toUpdateCause(), updateChecker, downloader, true);

        int delayInt = 60 + ThreadLocalRandom.current().nextInt(240 - 60 + 1);
        adapter.runRepeatAsync(() -> updateCheckExecutor.checkUpdate(UpdateCause.SCHEDULED, updateChecker, downloader, false), delayInt, delayInt, TimeUnit.MINUTES);
    }

    public enum InitCause {
        STARTUP,
        ERROR;

        public UpdateCause toUpdateCause() {
            return this == STARTUP ? UpdateCause.STARTUP : UpdateCause.ERROR;
        }
    }
}
