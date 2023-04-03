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
package net.skinsrestorer.shared.commands.library;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.commands.library.annotations.*;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.subjects.PermissionRegistry;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class CommandManager<T extends SRCommandSender> {
    private final Map<String, Predicate<T>> conditions = new HashMap<>();
    private final CommandDispatcher<T> dispatcher = new CommandDispatcher<>();
    private final CommandPlatform<T> platform;
    @Getter
    private final CommandExecutor<T> executor;

    public CommandManager(CommandPlatform<T> platform) {
        this.platform = platform;
        this.executor = new CommandExecutor<>(dispatcher, platform);

        // Register default conditions
        registerCondition("player-only", sender -> {
            if (sender instanceof SRPlayer) {
                return true;
            }

            sender.sendMessage(Message.ONLY_ALLOWED_ON_PLAYER);
            return false;
        });

        registerCondition("console-only", sender -> {
            if (sender instanceof SRPlayer) {
                sender.sendMessage(Message.ONLY_ALLOWED_ON_CONSOLE);
                return false;
            }

            return true;
        });
    }

    @SafeVarargs
    private static <T> Set<T> copyAndInsert(Set<T> set, T... values) {
        Set<T> copy = new LinkedHashSet<>(set);
        copy.addAll(Arrays.asList(values));
        return copy;
    }

    public void registerCommand(Object command) {
        CommandNames names = getAnnotation(CommandNames.class, command.getClass()).orElseThrow(() -> new IllegalStateException("Command is missing @CommandNames annotation"));

        String rootName = names.value()[0];

        String[] aliases;
        if (names.value().length == 1) {
            aliases = new String[0];
        } else {
            aliases = Arrays.copyOfRange(names.value(), 1, names.value().length);
        }

        PermissionRegistry rootPermission = getAnnotation(CommandPermission.class, command.getClass())
                .map(CommandPermission::value).orElseThrow(() -> new IllegalStateException("Command is missing @CommandPermission annotation"));

        LiteralArgumentBuilder<T> rootNode = LiteralArgumentBuilder.literal(rootName);

        rootNode.requires(requirePermission(rootPermission));

        Set<String> conditionTrail = new LinkedHashSet<>();

        getAnnotation(CommandConditions.class, command.getClass())
                .ifPresent(condition -> conditionTrail.addAll(Arrays.asList(condition.value())));

        addMethodCommands(rootNode, conditionTrail, command, command.getClass());

        LiteralCommandNode<T> rootCommandNode = dispatcher.register(rootNode);

        for (String alias : aliases) {
            dispatcher.register(LiteralArgumentBuilder.<T>literal(alias).redirect(rootCommandNode));
        }

        platform.registerCommand(new PlatformRegistration<>(rootName, aliases, rootPermission.getPermission().getPermissionString(), executor));
    }

    private void addMethodCommands(ArgumentBuilder<T, ?> node, Set<String> conditionTrail, Object command, Class<?> commandClass) {
        for (Method method : commandClass.getDeclaredMethods()) {
            method.setAccessible(true);

            Optional<RootCommand> def = getAnnotation(RootCommand.class, method);

            if (def.isPresent()) {
                validateMethod(method);

                Set<String> commandConditions = insertPlayerCondition(insertAnnotationConditions(conditionTrail, method), method);
                Command<T> defaultCommand = requireConditions(source -> {
                    try {
                        method.invoke(command, source);
                        return Command.SINGLE_SUCCESS;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return 0;
                    }
                }, commandConditions);

                if (method.getParameterTypes().length == 1) {
                    if (node.getCommand() != null) {
                        throw new IllegalStateException("Default command already set");
                    }

                    node.executes(defaultCommand);
                } else { // 2+ parameters
                    // TODO: Add support for arguments
                }

                continue;
            }

            Optional<Subcommand> names = getAnnotation(Subcommand.class, method);

            if (names.isPresent()) {
                validateMethod(method);

                String[] subcommandNames = names.get().value();

                for (String subCommandName : subcommandNames) {
                    LiteralArgumentBuilder<T> childNode = LiteralArgumentBuilder.literal(subCommandName);

                    Set<String> commandConditions = insertPlayerCondition(insertAnnotationConditions(conditionTrail, method), method);

                    childNode.executes(requireConditions(source -> {
                        try {
                            method.invoke(command, source); // TODO: Add support for arguments
                            return Command.SINGLE_SUCCESS;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return 0;
                        }
                    }, commandConditions));

                    node.then(childNode);
                }
            }
        }
    }

    private void validateMethod(Method method) {
        if (method.getParameterTypes().length < 1 || !SRCommandSender.class.isAssignableFrom(method.getParameterTypes()[0])) {
            throw new IllegalStateException(
                    String.format("Method %s must have at least a single parameter of type SRCommandSender", method.getName()));
        }
    }

    private <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass, AnnotatedElement objectClass) {
        A annotation = objectClass.getAnnotation(annotationClass);
        if (annotation == null) {
            return Optional.empty();
        }

        return Optional.of(annotation);
    }

    private Predicate<T> requirePermission(PermissionRegistry permission) {
        return source -> source.hasPermission(permission);
    }

    private Command<T> requireConditions(Command<T> command, Iterable<String> conditions) {
        return source -> {
            if (checkConditions(source.getSource(), conditions)) {
                return command.run(source);
            }

            return 0;
        };
    }

    private Set<String> insertAnnotationConditions(Set<String> conditionTrail, AnnotatedElement element) {
        Set<String> copy = copyAndInsert(conditionTrail);
        getAnnotation(CommandConditions.class, element)
                .ifPresent(condition -> copy.addAll(Arrays.asList(condition.value())));
        return copy;
    }

    private Set<String> insertPlayerCondition(Set<String> conditionTrail, Method method) {
        if (method.getParameterTypes()[0] == SRPlayer.class) {
            return copyAndInsert(conditionTrail, "player-only");
        }

        return copyAndInsert(conditionTrail);
    }

    /**
     * Register a condition that can be used in the @Conditions annotation
     *
     * @param name      Name of the condition
     * @param condition Predicate that returns true if the condition is met
     */
    public void registerCondition(String name, Predicate<T> condition) {
        conditions.put(name, condition);
    }

    protected boolean checkConditions(T source, Iterable<String> conditions) {
        for (String condition : conditions) {
            if (!this.conditions.get(condition).test(source)) {
                return false;
            }
        }

        return true;
    }
}
