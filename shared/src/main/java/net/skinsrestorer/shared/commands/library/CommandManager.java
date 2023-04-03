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
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
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
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
                registerParameters(node, commandConditions, command, method);

                continue;
            }

            Optional<Subcommand> names = getAnnotation(Subcommand.class, method);

            if (names.isPresent()) {
                validateMethod(method);

                String[] subcommandNames = names.get().value();

                for (String subCommandName : subcommandNames) {
                    LiteralArgumentBuilder<T> childNode = LiteralArgumentBuilder.literal(subCommandName);

                    Set<String> commandConditions = insertPlayerCondition(insertAnnotationConditions(conditionTrail, method), method);

                    registerParameters(childNode, commandConditions, command, method);
                }
            }
        }
    }

    private void registerParameters(ArgumentBuilder<T, ?> node, Set<String> conditionTrail, Object command, Method method) {
        List<ArgumentBuilder<T, ?>> nodes = new ArrayList<>();
        nodes.add(node);
        int i = 0;
        for (Parameter parameter : method.getParameters()) {
            if (i == 0) {
                i++;
                continue;
            }

            // Implementing support for other types is easy, just add a new if statement
            if (parameter.getType() == String.class) {
                nodes.add(RequiredArgumentBuilder.argument(parameter.getName(), StringArgumentType.word()));
            } else if (parameter.getType() == int.class) {
                nodes.add(RequiredArgumentBuilder.argument(parameter.getName(), IntegerArgumentType.integer()));
            } else if (Enum.class.isAssignableFrom(parameter.getType())) {
                nodes.add(RequiredArgumentBuilder.argument(parameter.getName(), new ArgumentType<Enum<?>>() {
                    @Override
                    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                        return Suggestions.empty(); // TODO
                    }

                    @Override
                    public Collection<String> getExamples() {
                        List<String> examples = new ArrayList<>();
                        for (Object constant : parameter.getType().getEnumConstants()) {
                            System.out.println(constant);
                            examples.add(constant.toString());
                        }
                        return examples;
                    }

                    @Override
                    public Enum<?> parse(StringReader reader) throws CommandSyntaxException {
                        final int start = reader.getCursor();
                        final String string = reader.readString();
                        try {
                            System.out.println(string);
                            return Enum.valueOf((Class<Enum>) parameter.getType(), string.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                            reader.setCursor(start);
                            throw new SimpleCommandExceptionType(new LiteralMessage("Invalid enum value")).createWithContext(reader);
                        }
                    }
                }));
            } else {
                continue;
                // TODO
                // throw new IllegalStateException("Unsupported parameter type: " + parameter.getType().getName());
            }

            i++;
        }

        nodes.get(nodes.size() - 1).executes(requireConditions(context -> {
            try {
                List<Object> args = new ArrayList<>();
                args.add(context.getSource());
                int i1 = 0;
                for (Parameter parameter : method.getParameters()) {
                    if (i1 == 0) {
                        i1++;
                        continue;
                    }
                    args.add(context.getArgument(parameter.getName(), parameter.getType()));
                }
                method.invoke(command, args.toArray());
                return Command.SINGLE_SUCCESS;
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }, conditionTrail));

        if (nodes.size() == 1) {
            return;
        }

        for (int j = 0; j < nodes.size() - 1; j++) {
            nodes.get(j).then(nodes.get(j + 1));
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
