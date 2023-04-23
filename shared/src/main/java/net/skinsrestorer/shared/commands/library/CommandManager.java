/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.skinsrestorer.shared.commands.library.annotations.*;
import net.skinsrestorer.shared.commands.library.types.EnumArgumentType;
import net.skinsrestorer.shared.commands.library.types.SRPlayerArgumentType;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;
import net.skinsrestorer.shared.utils.FluentList;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Predicate;

public class CommandManager<T extends SRCommandSender> {
    public final CommandDispatcher<T> dispatcher = new CommandDispatcher<>();
    private final Map<String, Predicate<T>> conditions = new HashMap<>();
    private final CommandPlatform<T> platform;
    private final SRLogger logger;
    private final SkinsRestorerLocale locale;

    public CommandManager(CommandPlatform<T> platform, SRLogger logger, SkinsRestorerLocale locale) {
        this.platform = platform;
        this.logger = logger;
        this.locale = locale;

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

        addMethodCommands(rootNode, rootPermission, conditionTrail, command, command.getClass());

        LiteralCommandNode<T> rootCommandNode = dispatcher.register(rootNode);

        for (String alias : aliases) {
            dispatcher.getRoot().addChild(buildRedirect(alias, rootCommandNode));
        }

        String description = locale.getMessage(locale.getDefaultForeign(),
                getAnnotation(Description.class, command.getClass())
                        .orElseThrow(() -> new IllegalStateException("Command is missing @Description annotation")).value());

        String[] usage = dispatcher.getAllUsage(rootCommandNode, null, false);

        for (int i = 0; i < usage.length; i++) {
            usage[i] = "/" + rootName + " " + usage[i];
        }

        SRCommandMeta meta = new SRCommandMeta(rootName, aliases, rootPermission.getPermission(), description);
        CommandExecutor<T> executor = new CommandExecutor<>(dispatcher, this, meta, logger);
        SRRegisterPayload<T> payload = new SRRegisterPayload<>(meta, executor);

        platform.registerCommand(payload);
    }

    private void addMethodCommands(ArgumentBuilder<T, ?> node, PermissionRegistry rootPermission, Set<String> conditionTrail, Object command, Class<?> commandClass) {
        for (Method method : commandClass.getDeclaredMethods()) {
            method.setAccessible(true);

            Optional<RootCommand> def = getAnnotation(RootCommand.class, method);
            Optional<Subcommand> names = getAnnotation(Subcommand.class, method);
            if (names.isPresent() || def.isPresent()) {
                validateMethod(method);

                Set<String> commandConditions = insertPlayerCondition(insertAnnotationConditions(conditionTrail, method), method);
                if (def.isPresent()) {
                    registerParameters(node, rootPermission, commandConditions, command, method);
                } else {
                    String[] namesArray = names.get().value();
                    if (namesArray.length == 0) {
                        throw new IllegalStateException("Subcommand annotation must have at least one name");
                    }

                    String name = namesArray[0];
                    String[] aliases = Arrays.copyOfRange(namesArray, 1, namesArray.length);

                    LiteralArgumentBuilder<T> childNode = LiteralArgumentBuilder.literal(name);

                    PermissionRegistry subPermission = getAnnotation(CommandPermission.class, method)
                            .map(CommandPermission::value).orElseThrow(() -> new IllegalStateException("Command is missing @CommandPermission annotation"));

                    childNode.requires(requirePermission(subPermission));

                    registerParameters(childNode, subPermission, commandConditions, command, method);

                    LiteralCommandNode<T> registeredNode = childNode.build();
                    node.then(registeredNode);

                    for (String alias : aliases) {
                        node.then(buildRedirect(alias, registeredNode));
                    }
                }
            }
        }
    }

    private void registerParameters(ArgumentBuilder<T, ?> node, PermissionRegistry subPermission, Set<String> conditionTrail, Object command, Method method) {
        List<ArgumentBuilder<T, ?>> nodes = new ArrayList<>();
        nodes.add(node);
        int i = 0;
        for (Parameter parameter : method.getParameters()) {
            if (i == 0) {
                i++;
                continue;
            }

            ArgumentType<?> argumentType;
            // Implementing support for other types is easy, just add a new if-statement
            if (parameter.getType() == String.class) {
                argumentType = new ArgumentType<String>() {
                    @Override
                    public String parse(StringReader reader) {
                        final int start = reader.getCursor();
                        final String string = reader.getString();
                        while (reader.canRead() && reader.peek() != ' ') {
                            reader.skip();
                        }
                        return string.substring(start, reader.getCursor());
                    }

                    @Override
                    public Collection<String> getExamples() {
                        return FluentList.listOf("example", "example2");
                    }
                };
            } else if (parameter.getType() == int.class) {
                argumentType = IntegerArgumentType.integer();
            } else if (Enum.class.isAssignableFrom(parameter.getType())) {
                argumentType = new EnumArgumentType(parameter.getType());
            } else if (parameter.getType().isAssignableFrom(SRPlayer.class)) {
                argumentType = new SRPlayerArgumentType(platform);
            } else {
                throw new IllegalStateException("Unsupported parameter type: " + parameter.getType().getName());
            }

            RequiredArgumentBuilder<T, ?> argumentBuilder = RequiredArgumentBuilder.argument(parameter.getName(), argumentType);
            argumentBuilder.requires(requirePermission(subPermission));
            nodes.add(argumentBuilder);

            i++;
        }

        nodes.get(nodes.size() - 1).executes(requireConditions(context -> {
            try {
                int i1 = 0;
                Object[] parameters = new Object[method.getParameterCount()];
                for (Parameter parameter : method.getParameters()) {
                    if (i1 == 0) {
                        parameters[i1] = context.getSource();
                        i1++;
                        continue;
                    }
                    parameters[i1] = context.getArgument(parameter.getName(), parameter.getType());
                    i1++;
                }
                logger.debug(String.format("Executing command %s with method parameters %s", method.getName(), Arrays.toString(parameters)));
                platform.runAsync(() -> {
                    try {
                        method.invoke(command, parameters);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                return Command.SINGLE_SUCCESS;
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }, conditionTrail));

        if (nodes.size() > 1) {
            for (int i1 = nodes.size() - 1; i1 > 0; i1--) {
                nodes.get(i1 - 1).then(nodes.get(i1));
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

    private boolean checkConditions(T source, Iterable<String> conditions) {
        for (String condition : conditions) {
            if (!this.conditions.get(condition).test(source)) {
                return false;
            }
        }

        return true;
    }

    public String[] getRootHelp(String command, T source) {
        String[] usage = dispatcher.getAllUsage(dispatcher.getRoot().getChild(command), source, true);

        for (int i = 0; i < usage.length; i++) {
            usage[i] = "/" + command + " " + usage[i];
        }

        return usage;
    }

    public void executeCommand(T executor, String input) {
        logger.debug(String.format("Executing command: '%s' for '%s'", input, executor));
        try {
            dispatcher.execute(input, executor);
        } catch (CommandSyntaxException e) {
            executor.sendMessage(e.getRawMessage().getString());
        }
    }

    // Taken from https://github.com/PaperMC/Velocity/blob/8abc9c80a69158ebae0121fda78b55c865c0abad/proxy/src/main/java/com/velocitypowered/proxy/util/BrigadierUtils.java#L38
    private LiteralCommandNode<T> buildRedirect(
            final String alias, final LiteralCommandNode<T> destination) {
        // Redirects only work for nodes with children, but break the top argument-less command.
        // Manually adding the root command after setting the redirect doesn't fix it.
        // See https://github.com/Mojang/brigadier/issues/46). Manually clone the node instead.
        LiteralArgumentBuilder<T> builder = LiteralArgumentBuilder
                .<T>literal(alias.toLowerCase(Locale.ENGLISH))
                .requires(destination.getRequirement())
                .forward(destination.getRedirect(), destination.getRedirectModifier(), destination.isFork())
                .executes(destination.getCommand());
        for (CommandNode<T> child : destination.getChildren()) {
            builder.then(child);
        }
        return builder.build();
    }
}
