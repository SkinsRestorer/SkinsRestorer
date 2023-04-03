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
        Map<String, Set<Method>> literalChildren = new HashMap<>();
        for (Method method : commandClass.getDeclaredMethods()) {
            method.setAccessible(true);

            Optional<Default> def = getAnnotation(Default.class, method);

            if (def.isPresent()) {
                if (node.getCommand() != null) {
                    throw new IllegalStateException("Default command already set");
                }

                if (method.getParameterTypes().length != 1 || !method.getParameterTypes()[0].isAssignableFrom(SRCommandSender.class)) {
                    throw new IllegalStateException("Default command must have a single parameter of type SRCommandSender");
                }

                Set<String> commandConditions = insertPlayerCondition(insertAnnotationConditions(conditionTrail, method), method);
                node.executes(requireConditions(source -> {
                    try {
                        method.invoke(command, source);
                        return Command.SINGLE_SUCCESS;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return 0;
                    }
                }, commandConditions));

                continue;
            }

            Optional<Subcommand> names = getAnnotation(Subcommand.class, method);

            if (names.isPresent()) {
                String[] subcommandNames = names.get().value();

                if (subcommandNames.length == 0) {
                    throw new IllegalStateException("Subcommand must have at least one name");
                }

                for (String subcommandName : subcommandNames) {
                    if (literalChildren.containsKey(subcommandName)) {
                        literalChildren.get(subcommandName).add(method);
                    } else {
                        Set<Method> methods = new HashSet<>();
                        methods.add(method);
                        literalChildren.put(subcommandName, methods);
                    }
                }
            }
        }

        for (Map.Entry<String, Set<Method>> entry : literalChildren.entrySet()) {
            LiteralArgumentBuilder<T> childNode = LiteralArgumentBuilder.literal(entry.getKey());

            for (Method method : entry.getValue()) {
                Set<String> commandConditions = insertPlayerCondition(insertAnnotationConditions(conditionTrail, method), method);


            }

            node.then(childNode);
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
        return source -> {
            if (permission == PermissionRegistry.EMPTY) {
                return true;
            }

            return source.hasPermission(permission);
        };
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

    @SafeVarargs
    private static <T> Set<T> copyAndInsert(Set<T> set, T... values) {
        Set<T> copy = new LinkedHashSet<>(set);
        copy.addAll(Arrays.asList(values));
        return copy;
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
