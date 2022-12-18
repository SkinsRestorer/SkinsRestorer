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
package net.skinsrestorer.shared.plugin;

import co.aikar.commands.CommandManager;
import co.aikar.locales.LocaleManager;
import lombok.Getter;
import net.skinsrestorer.shared.SkinsRestorerAPIShared;
import net.skinsrestorer.shared.interfaces.ISRForeign;
import net.skinsrestorer.shared.interfaces.ISRLogger;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.update.UpdateChecker;
import net.skinsrestorer.shared.update.UpdateCheckerGitHub;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.connections.MineSkinAPI;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.inventivetalent.update.spiget.UpdateCallback;

import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Getter
public abstract class SkinsRestorerShared implements ISRPlugin {
    protected final MetricsCounter metricsCounter = new MetricsCounter();
    protected final CooldownStorage cooldownStorage = new CooldownStorage();
    protected final SRLogger logger;
    protected final MojangAPI mojangAPI;
    protected final MineSkinAPI mineSkinAPI;
    protected final SkinStorage skinStorage;
    protected final LocaleManager<ISRForeign> localeManager;
    protected final UpdateChecker updateChecker;
    protected final Path dataFolder;
    protected final String version;
    protected CommandManager<?, ?, ?, ?, ?, ?> manager;
    protected boolean outdated = false;

    protected SkinsRestorerShared(ISRLogger isrLogger, boolean loggerColor, String version, String updateCheckerAgent, Path dataFolder) {
        this.logger = new SRLogger(isrLogger, loggerColor);
        this.mojangAPI = new MojangAPI(metricsCounter);
        this.mineSkinAPI = new MineSkinAPI(logger, metricsCounter);
        this.skinStorage = new SkinStorage(logger, mojangAPI, mineSkinAPI);
        this.localeManager = LocaleManager.create(ISRForeign::getLocale, Locale.ENGLISH);
        this.version = version;
        this.updateChecker = new UpdateCheckerGitHub(2124, version, logger, updateCheckerAgent);
        this.dataFolder = dataFolder;
    }

    protected void sharedInitCommands() {
        manager = createCommandManager();

        prepareACF(manager, logger);

        runRepeatAsync(cooldownStorage::cleanup, 60, 60, TimeUnit.SECONDS);
    }

    protected abstract CommandManager<?, ?, ?, ?, ?, ?> createCommandManager();

    protected abstract void registerAPI();

    protected abstract boolean isProxyMode();

    public void checkUpdate(boolean showUpToDate) {
        runAsync(() -> updateChecker.checkForUpdate(new UpdateCallback() {
            @Override
            public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                outdated = true;
                updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, version, isProxyMode())
                        .forEach(logger::info);
            }

            @Override
            public void upToDate() {
                if (!showUpToDate)
                    return;

                updateChecker.getUpToDateMessages(version, isProxyMode()).forEach(logger::info);
            }
        }));
    }
}
