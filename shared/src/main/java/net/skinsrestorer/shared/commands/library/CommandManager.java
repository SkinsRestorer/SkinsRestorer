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
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.commands.library.annotations.*;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.subjects.PermissionRegistry;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.utils.FluentList;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CommandManager<T extends SRCommandSender> {
    private final Map<String, Predicate<T>> conditions = new HashMap<>();
    public final CommandDispatcher<T> dispatcher = new CommandDispatcher<>();
    private final CommandPlatform<T> platform;
    private final SkinsRestorerLocale locale;
    @Getter
    private final CommandExecutor<T> executor;

    public CommandManager(CommandPlatform<T> platform, SkinsRestorerLocale locale) {
        this.platform = platform;
        this.locale = locale;
        this.executor = new CommandExecutor<>(dispatcher);

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

        Optional<PublicVisibility> publicVisibility = getAnnotation(PublicVisibility.class, command.getClass());

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

        platform.registerCommand(new PlatformRegistration<>(rootName, aliases,
                publicVisibility.isPresent() ? null : rootPermission.getPermission().getPermissionString(), description, usage, executor));
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

                    System.out.println("Registering subcommand " + name);
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
                argumentType = StringArgumentType.word();
            } else if (parameter.getType() == int.class) {
                argumentType = IntegerArgumentType.integer();
            } else if (Enum.class.isAssignableFrom(parameter.getType())) {
                argumentType = new ArgumentType<Enum<?>>() {
                    @Override
                    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                        for (String example : getExamples()) {
                            if (example.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                                builder.suggest(example);
                            }
                        }
                        return builder.buildFuture();
                    }

                    @SuppressWarnings("rawtypes")
                    @Override
                    public Collection<String> getExamples() {
                        return Arrays.stream(parameter.getType().getEnumConstants())
                                .map(o -> (Enum) o)
                                .map(Enum::name).collect(Collectors.toList());
                    }

                    @SuppressWarnings({"unchecked", "rawtypes"})
                    @Override
                    public Enum<?> parse(StringReader reader) throws CommandSyntaxException {
                        final int start = reader.getCursor();
                        final String string = reader.readString();
                        try {
                            return Enum.valueOf((Class<Enum>) parameter.getType(), string.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            reader.setCursor(start);
                            throw new SimpleCommandExceptionType(new LiteralMessage("Invalid enum value")).createWithContext(reader);
                        }
                    }
                };
            } else if (parameter.getType().isAssignableFrom(SRPlayer.class)) {
                argumentType = new ArgumentType<SRPlayer>() {
                    @Override
                    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                        for (SRPlayer player : platform.getOnlinePlayers()) {
                            if (player.getName().toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                                if (context.getSource() instanceof SRPlayer && !((SRPlayer) context.getSource()).canSee(player)) {
                                    continue;
                                }

                                builder.suggest(player.getName());
                            }
                        }
                        return builder.buildFuture();
                    }

                    @Override
                    public Collection<String> getExamples() {
                        return FluentList.listOf("Pistonmaster", "xknat");
                    }

                    @Override
                    public SRPlayer parse(StringReader reader) throws CommandSyntaxException {
                        final int start = reader.getCursor();
                        final String string = reader.readString();

                        Optional<SRPlayer> exactPlayer = platform.getOnlinePlayers().stream()
                                .filter(p -> p.getName().equals(string))
                                .findFirst();

                        if (exactPlayer.isPresent()) {
                            return exactPlayer.get();
                        }

                        Optional<SRPlayer> player = platform.getOnlinePlayers().stream()
                                .filter(p -> p.getName().equalsIgnoreCase(string.toLowerCase()))
                                .findFirst();

                        if (player.isPresent()) {
                            return player.get();
                        }

                        reader.setCursor(start);
                        throw new SimpleCommandExceptionType(new LiteralMessage("Unknown player")).createWithContext(reader);
                    }
                };
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
