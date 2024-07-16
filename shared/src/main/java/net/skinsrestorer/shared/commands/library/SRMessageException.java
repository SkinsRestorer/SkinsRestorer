package net.skinsrestorer.shared.commands.library;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.utils.ComponentString;

import java.util.function.Function;

@Getter
@RequiredArgsConstructor
public class SRMessageException extends IllegalArgumentException {
    private final Function<SkinsRestorerLocale, ComponentString> messageSupplier;
}
