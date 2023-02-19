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
package net.skinsrestorer.shared.update;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.interfaces.SRPlatformAdapter;
import net.skinsrestorer.shared.platform.SRPlugin;
import net.skinsrestorer.shared.utils.log.SRLogger;

import javax.inject.Inject;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SharedUpdateCheck implements UpdateCheck {
    private final SRPlugin plugin;
    private final SRPlatformAdapter adapter;
    private final UpdateCheckerGitHub updateChecker;
    private final SRLogger logger;

    @Override
    public void run() {
        checkUpdate(true);

        int delayInt = 60 + ThreadLocalRandom.current().nextInt(240 - 60 + 1);
        adapter.runRepeatAsync(() -> checkUpdate(false), delayInt, delayInt, TimeUnit.MINUTES);
    }

    public void checkUpdate(boolean showUpToDate) {
        if (SRPlugin.isUnitTest()) {
            if (showUpToDate) {
                logger.info("Unit test mode, not checking for updates.");
                updateChecker.getUpToDateMessages().forEach(logger::info);
            }
            return;
        }

        adapter.runAsync(() -> updateChecker.checkForUpdate(new UpdateCallback() {
            @Override
            public void updateAvailable(String newVersion, String downloadUrl) {
                plugin.setOutdated();
                updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, false).forEach(logger::info);
            }

            @Override
            public void upToDate() {
                if (!showUpToDate) {
                    return;
                }

                updateChecker.getUpToDateMessages().forEach(logger::info);
            }
        }));
    }
}
