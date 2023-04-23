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
package net.skinsrestorer.bukkit.update;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.update.UpdateCause;
import net.skinsrestorer.shared.update.UpdateCheckExecutor;
import net.skinsrestorer.shared.update.UpdateCheckInit;
import net.skinsrestorer.shared.update.UpdateCheckerGitHub;

import javax.inject.Inject;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BukkitUpdateCheckInit implements UpdateCheckInit {
    private final SRPlatformAdapter<?> adapter;
    private final UpdateCheckerGitHub updateChecker;
    private final UpdateDownloaderGithub downloader;
    private final UpdateCheckExecutor updateCheckExecutor;

    @Override
    public void run(InitCause cause) {
        updateCheckExecutor.checkUpdate(cause.toUpdateCause(), updateChecker, downloader);

        // Delay update between 5 & 30 minutes
        int delayInt = 300 + ThreadLocalRandom.current().nextInt(1800 + 1 - 300);
        // Repeat update between 1 & 4 hours
        int periodInt = 60 * (60 + ThreadLocalRandom.current().nextInt(240 + 1 - 60));
        adapter.runRepeatAsync(() -> updateCheckExecutor.checkUpdate(UpdateCause.SCHEDULED, updateChecker, downloader), delayInt, periodInt, TimeUnit.SECONDS);
    }
}
