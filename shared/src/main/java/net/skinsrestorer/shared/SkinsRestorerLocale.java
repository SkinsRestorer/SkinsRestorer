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
package net.skinsrestorer.shared;

import ch.jalu.configme.SettingsManager;
import co.aikar.locales.LocaleManager;
import lombok.Getter;
import net.skinsrestorer.shared.config.MessageConfig;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.subjects.SRForeign;
import net.skinsrestorer.shared.utils.C;

import javax.inject.Inject;
import java.text.MessageFormat;

public class SkinsRestorerLocale {
    @Inject
    private LocaleManager<SRForeign> localeManager;
    @Inject
    private SettingsManager settings;
    @Getter
    private final SRForeign defaultForeign = () -> settings.getProperty(MessageConfig.LANGUAGE);

    public String getMessage(SRForeign foreign, Message key, Object... args) {
        String message = localeManager.getMessage(foreign, key.getKey());

        if (message.contains("{prefix}")) {
            if (settings.getProperty(MessageConfig.DISABLE_PREFIX)) {
                // Extra space in pattern to remove space from start of message
                message = message.replace("{prefix} ", "");
            } else {
                message = message.replace("{prefix}", localeManager.getMessage(foreign, Message.PREFIX.getKey()));
            }
        }

        return new MessageFormat(C.c(message)).format(args);
    }
}
