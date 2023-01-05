/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
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
import net.skinsrestorer.shared.interfaces.SRPlatformAdapter;
import net.skinsrestorer.shared.platform.SRPlugin;
import net.skinsrestorer.shared.update.UpdateCheck;
import net.skinsrestorer.shared.update.UpdateCheckerGitHub;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.inventivetalent.update.spiget.UpdateCallback;

import javax.inject.Inject;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BukkitUpdateCheck implements UpdateCheck {
    private final SRPlugin plugin;
    private final SRPlatformAdapter adapter;
    private final UpdateCheckerGitHub updateChecker;
    private final SRLogger logger;
    private final UpdateDownloaderGithub downloader;
    private boolean updateDownloaded;

    @Override
    public void run() {
        checkUpdate(true);

        // Delay update between 5 & 30 minutes
        int delayInt = 300 + ThreadLocalRandom.current().nextInt(1800 + 1 - 300);
        // Repeat update between 1 & 4 hours
        int periodInt = 60 * (60 + ThreadLocalRandom.current().nextInt(240 + 1 - 60));
        adapter.runRepeatAsync(() -> checkUpdate(false), delayInt, periodInt, TimeUnit.SECONDS);
    }

    public void checkUpdate(boolean showUpToDate) {
        if (plugin.isUnitTest()) {
            if (showUpToDate) {
                logger.info("Unit test mode, not checking for updates.");
                updateChecker.getUpToDateMessages().forEach(logger::info);
            }
            return;
        }

        adapter.runAsync(() -> updateChecker.checkForUpdate(new UpdateCallback() {
            @Override
            public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                plugin.setOutdated();
                if (updateDownloaded) {
                    return;
                }

                String failReason = null;
                if (hasDirectDownload) {
                    if (downloader.downloadUpdate()) {
                        updateDownloaded = true;
                    } else {
                        failReason = downloader.getFailReason().toString();
                    }
                }

                updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, true, failReason).forEach(logger::info);
            }

            @Override
            public void upToDate() {
                if (!showUpToDate)
                    return;

                updateChecker.getUpToDateMessages().forEach(logger::info);
            }
        }));
    }
}
