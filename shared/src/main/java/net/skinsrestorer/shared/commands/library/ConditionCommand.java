package net.skinsrestorer.shared.commands.library;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.subjects.SRCommandSender;

import java.util.List;

@RequiredArgsConstructor
public class ConditionCommand<T extends SRCommandSender> implements Command<T> {
    private final List<ConditionRegistration<T>> conditions;
    private final Command<T> delegate;

    @Override
    public int run(CommandContext<T> context) throws CommandSyntaxException {
        for (ConditionRegistration<T> condition : conditions) {
            if (!condition.getCondition().test(context.getSource())) {
                return 0;
            }
        }

        return delegate.run(context);
    }
}
