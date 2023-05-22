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
import java.util.stream.Collectors;

public class CommandManager<T extends SRCommandSender> {
    public static final String ARGUMENT_SEPARATOR = " ";

    private final CommandDispatcher<T> dispatcher = new CommandDispatcher<>();
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
        LiteralArgumentBuilder<T> rootBuilder = LiteralArgumentBuilder.literal("root");

        addClassCommands(rootBuilder, new LinkedHashSet<>(), command, command.getClass());

        CommandNode<T> rootNode = rootBuilder.build();

        CommandNode<T> baseNode = rootNode.getChildren().iterator().next();
        String commandName = baseNode.getName();
        List<String> aliases = new ArrayList<>();
        for (CommandNode<T> node : rootNode.getChildren()) {
            if (node.getName().equals(commandName)) {
                continue;
            }

            aliases.add(node.getName());
        }

        for (CommandNode<T> node : rootNode.getChildren()) {
            dispatcher.getRoot().addChild(node);
        }

        String[] usage = dispatcher.getAllUsage(rootNode, null, false);

        for (int i = 0; i < usage.length; i++) {
            usage[i] = "/" + usage[i];
        }

        SRCommandMeta<T> meta = new SRCommandMeta<>(commandName,
                aliases.toArray(new String[0]), baseNode::canUse, ((CommandInjectHelp<T>) baseNode.getCommand()).getHelpData());
        CommandExecutor<T> executor = new CommandExecutor<>(dispatcher, this, meta, logger);
        SRRegisterPayload<T> payload = new SRRegisterPayload<>(meta, executor);

        platform.registerCommand(payload);
    }

    private void addClassCommands(ArgumentBuilder<T, ?> baseNode, Set<String> conditionTrail, Object command, Class<?> commandClass) {
        CommandNames names = getAnnotation(CommandNames.class, command.getClass())
                .orElseThrow(() ->
                        new IllegalStateException("Command is missing @CommandNames annotation"));

        String mainName = names.value()[0];

        String[] aliases;
        if (names.value().length == 1) {
            aliases = new String[0];
        } else {
            aliases = Arrays.copyOfRange(names.value(), 1, names.value().length);
        }

        PermissionRegistry classPermission = getAnnotation(CommandPermission.class, command.getClass())
                .map(CommandPermission::value).orElseThrow(() ->
                        new IllegalStateException(String.format("Command %s is missing @CommandPermission annotation", mainName)));

        LiteralArgumentBuilder<T> classBuilder = LiteralArgumentBuilder.literal(mainName);

        classBuilder.requires(requirePermission(classPermission));

        Set<String> classConditionTrail = new LinkedHashSet<>(conditionTrail);

        getAnnotation(CommandConditions.class, command.getClass())
                .ifPresent(condition -> classConditionTrail.addAll(Arrays.asList(condition.value())));

        CommandHelpData currentHelpData = getHelpData(mainName, command.getClass());

        addMethodCommands(classBuilder, classPermission, classConditionTrail, command, command.getClass(), currentHelpData);

        LiteralCommandNode<T> classCommandNode = classBuilder.build();
        baseNode.then(classCommandNode);

        for (String alias : aliases) {
            baseNode.then(buildRedirect(alias, classCommandNode));
        }
    }

    private void addMethodCommands(ArgumentBuilder<T, ?> node, PermissionRegistry rootPermission,
                                   Set<String> conditionTrail, Object command, Class<?> commandClass, CommandHelpData currentHelpData) {
        List<Method> sortedMethods = new ArrayList<>();
        for (Method method : commandClass.getDeclaredMethods()) {
            method.setAccessible(true);
            sortedMethods.add(method);
        }

        sortedMethods.sort(Comparator.comparingInt(Method::getParameterCount));
        Collections.reverse(sortedMethods);

        for (Method method : sortedMethods) {
            Optional<RootCommand> def = getAnnotation(RootCommand.class, method);
            Optional<Subcommand> names = getAnnotation(Subcommand.class, method);
            if (!def.isPresent() && !names.isPresent()) {
                continue;
            }

            validateMethod(method);

            Set<String> commandConditions = insertPlayerCondition(insertAnnotationConditions(conditionTrail, method), method);
            if (def.isPresent()) {
                registerParameters(node, rootPermission, commandConditions, command, method, currentHelpData);
            } else {
                String[] namesArray = names.get().value();
                if (namesArray.length == 0) {
                    throw new IllegalStateException("Subcommand annotation must have at least one name");
                }

                String name = namesArray[0];
                String[] aliases = Arrays.copyOfRange(namesArray, 1, namesArray.length);

                LiteralArgumentBuilder<T> childNode = LiteralArgumentBuilder.literal(name);

                PermissionRegistry subPermission = getAnnotation(CommandPermission.class, method)
                        .map(CommandPermission::value).orElseThrow(() ->
                                new IllegalStateException(String.format("Command %s is missing @CommandPermission annotation", method.getName())));

                childNode.requires(requirePermission(subPermission));

                CommandHelpData commandHelpData = getHelpData(name, method);

                registerParameters(childNode, subPermission, commandConditions, command, method, commandHelpData);

                LiteralCommandNode<T> registeredNode = childNode.build();
                node.then(registeredNode);

                for (String alias : aliases) {
                    node.then(buildRedirect(alias, registeredNode));
                }
            }
        }
    }

    private CommandHelpData getHelpData(String nodeName, AnnotatedElement element) {
        Optional<Private> privateAnnotation = getAnnotation(Private.class, element);

        if (privateAnnotation.isPresent()) {
            return new CommandHelpData(true, null);
        } else {
            Message description = getAnnotation(Description.class, element)
                    .orElseThrow(() ->
                            new IllegalStateException(String.format("Command %s is missing @Description annotation", nodeName))).value();

            return new CommandHelpData(false, description);
        }
    }

    private void registerParameters(ArgumentBuilder<T, ?> node, PermissionRegistry subPermission,
                                    Set<String> conditionTrail, Object command, Method method, CommandHelpData currentHelpData) {
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

        nodes.get(nodes.size() - 1).executes(new CommandInjectHelp<>(currentHelpData,
                new ConditionCommand<>(getConditionRegistrations(conditionTrail),
                        new BrigadierCommand<>(method, logger, command, platform))));

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

    private List<ConditionRegistration<T>> getConditionRegistrations(Set<String> conditionTrail) {
        List<ConditionRegistration<T>> result = new ArrayList<>();
        for (String condition : conditionTrail) {
            if (!conditions.containsKey(condition)) {
                throw new IllegalStateException("Unknown condition: " + condition);
            }

            result.add(new ConditionRegistration<>(condition, conditions.get(condition)));
        }

        return result;
    }

    public List<String> getHelpMessage(String command, T source) {
        return getHelpMessageNodeStart(dispatcher.getRoot().getChild(command), "/" + command, source);
    }

    protected List<String> getHelpMessageNodeStart(CommandNode<T> node, String commandPrefix, T source) {
        List<String> result = new ArrayList<>();
        getAllUsage(node, source, result, "");
        result.replaceAll(s -> commandPrefix + ARGUMENT_SEPARATOR + s);
        return Collections.unmodifiableList(result.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList()));
    }

    private void getAllUsage(CommandNode<T> node, T source, List<String> result, String prefix) {
        if (!node.canUse(source)) {
            return;
        }

        if (node.getCommand() != null && ((CommandInjectHelp<T>) node.getCommand()).getHelpData().isPrivateCommand()) {
            return;
        }

        if (node.getCommand() != null && !prefix.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            builder.append(prefix);
            CommandInjectHelp<T> command = (CommandInjectHelp<T>) node.getCommand();
            if (command.getHelpData() != null) {
                builder.append(" - ");
                builder.append(locale.getMessage(source, command.getHelpData().getCommandDescription()));
            }
            result.add(builder.toString());
        }

        if (node.getRedirect() != null) {
            String redirect = node.getRedirect() == dispatcher.getRoot() ? "..." : "-> " + node.getRedirect().getUsageText();
            result.add(prefix.isEmpty() ? node.getUsageText() + ARGUMENT_SEPARATOR + redirect : prefix + ARGUMENT_SEPARATOR + redirect);
        } else if (!node.getChildren().isEmpty()) {
            for (CommandNode<T> child : node.getChildren()) {
                StringBuilder builder = new StringBuilder();
                if (!prefix.isEmpty()) {
                    builder.append(prefix).append(ARGUMENT_SEPARATOR);
                }

                builder.append(child.getUsageText());
                getAllUsage(child, source, result, builder.toString());
            }
        }
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
