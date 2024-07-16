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

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.config.AdvancedConfig;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class UpdateCheckExecutor {
    private final SRPlatformAdapter adapter;
    private final SettingsManager settings;

    public void checkUpdate(UpdateCause cause, UpdateCheckerGitHub updateChecker, UpdateDownloader downloader, boolean isSync) {
        if (settings.getProperty(AdvancedConfig.NO_CONNECTIONS)) {
            updateChecker.printUpToDate(UpdateCause.NO_NETWORK);
            return;
        }

        Runnable check = () -> updateChecker.checkForUpdate(cause, downloader);
        if (isSync) {
            adapter.runAsync(check);
        } else {
            check.run();
        }
    }
}
