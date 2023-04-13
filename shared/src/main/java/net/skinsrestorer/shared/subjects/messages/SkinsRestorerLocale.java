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
package net.skinsrestorer.shared.subjects.messages;

import ch.jalu.configme.SettingsManager;
import co.aikar.locales.LocaleManager;
import lombok.Getter;
import net.skinsrestorer.shared.config.MessageConfig;
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
    private final SRForeign defaultForeign = () -> settings.getProperty(MessageConfig.LOCALE);

    public String getMessage(SRForeign foreign, Message key, Object... args) {
        SRForeign target = settings.getProperty(MessageConfig.PER_ISSUER_LOCALE) ? foreign : defaultForeign;
        String message = localeManager.getMessage(target, key.getKey());

        if (message == null) {
            throw new IllegalStateException(String.format("Message %s not found", key.name()));
        }

        if (key.isPrefixed() && !settings.getProperty(MessageConfig.DISABLE_PREFIX)) {
            message = getMessage(target, Message.PREFIX_FORMAT, message);
        }

        return new MessageFormat(C.c(message)).format(args);
    }
}
