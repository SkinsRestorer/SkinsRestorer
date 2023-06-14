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
package net.skinsrestorer.shared.update;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.config.AdvancedConfig;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class UpdateCheckExecutor {
    private final SRPlugin plugin;
    private final SRPlatformAdapter<?> adapter;
    private final SettingsManager settings;
    private boolean updateDownloaded;

    public void checkUpdate(UpdateCause cause, UpdateCheckerGitHub updateChecker, UpdateDownloader downloader) {
        if (settings.getProperty(AdvancedConfig.NO_CONNECTIONS)) {
            updateChecker.printUpToDate(UpdateCause.NO_NETWORK);
            return;
        }

        adapter.runAsync(() -> updateChecker.checkForUpdate(new UpdateCallback() {
            @Override
            public void updateAvailable(String newVersion, String downloadUrl) {
                plugin.setOutdated();
                if (updateDownloaded) {
                    return;
                }

                if (downloader != null && downloader.downloadUpdate(downloadUrl)) {
                    updateDownloaded = true;
                }

                updateChecker.printUpdateAvailable(cause, newVersion, downloadUrl, downloader != null);
            }

            @Override
            public void upToDate() {
                if (cause == UpdateCause.SCHEDULED) {
                    return;
                }

                updateChecker.printUpToDate(cause);
            }
        }));
    }
}
