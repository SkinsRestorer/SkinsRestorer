package net.skinsrestorer.shared;

import ch.jalu.configme.SettingsManager;
import co.aikar.locales.LocaleManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.interfaces.ISRForeign;
import net.skinsrestorer.shared.interfaces.MessageKeyGetter;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.LocaleParser;

import java.text.MessageFormat;

@RequiredArgsConstructor
public class SkinsRestorerLocale {
    @Getter
    private static final ISRForeign defaultForeign = LocaleParser::getDefaultLocale;
    private final LocaleManager<ISRForeign> localeManager;
    private final SettingsManager settings;

    public String getMessage(ISRForeign foreign, MessageKeyGetter key, Object... args) {
        String message = localeManager.getMessage(foreign, key.getKey());

        if (message.contains("{prefix}")) {
            if (settings.getProperty(Config.DISABLE_PREFIX)) {
                // Extra space in pattern to remove space from start of message
                message = message.replace("{prefix} ", "");
            } else {
                message = message.replace("{prefix}", localeManager.getMessage(foreign, Message.PREFIX.getKey()));
            }
        }

        return C.c(new MessageFormat(message).format(args));
    }
}
