package net.skinsrestorer.shared.commands.library;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.subjects.SRCommandSender;

import java.util.function.Predicate;

@Getter
@RequiredArgsConstructor
public class ConditionRegistration<T extends SRCommandSender> {
    private final String name;
    private final Predicate<T> condition;
}
