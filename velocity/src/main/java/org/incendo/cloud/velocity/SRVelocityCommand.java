package org.incendo.cloud.velocity;

import com.velocitypowered.api.command.RawCommand;
import io.leangen.geantyref.GenericTypeReflector;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.internal.CommandNode;
import org.incendo.cloud.permission.Permission;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.Suggestions;
import org.incendo.cloud.util.StringUtils;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SRVelocityCommand<C> implements RawCommand {

    private final SRVelocityCommandManager<C> manager;
    private final CommandComponent<C> command;

    SRVelocityCommand(
            final @NonNull CommandComponent<C> command,
            final @NonNull SRVelocityCommandManager<C> manager
    ) {
        this.command = command;
        this.manager = manager;
    }

    @Override
    public void execute(Invocation invocation) {
        var cmd = this.command.name();
        if (!invocation.arguments().isEmpty()) {
            cmd += " " + invocation.arguments();
        }

        final C sender = this.manager.senderMapper().map(invocation.source());
        this.manager.commandExecutor().executeCommand(sender, cmd);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return this.suggestAsync(invocation).join();
    }

    /**
     * Velocity calls this on the Netty event loop of the requesting player, so it must not wait for suggestions.
     */
    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        var cmd = this.command.name() + " " + invocation.arguments();
        return this.manager.suggestionFactory()
                .suggest(this.manager.senderMapper().map(invocation.source()), cmd)
                .thenApply(SRVelocityCommand::mapSuggestions);
    }

    private static List<String> mapSuggestions(Suggestions<?, ?> result) {
        return result.list().stream()
                .map(Suggestion::suggestion)
                .map(suggestion -> StringUtils.trimBeforeLastSpace(suggestion, result.commandInput()))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        final CommandNode<C> node = this.namedNode();
        if (node == null) {
            return false;
        }

        final Map<Type, Permission> accessMap =
                node.nodeMeta().getOrDefault(CommandNode.META_KEY_ACCESS, Collections.emptyMap());
        final C cloudSender = this.manager.senderMapper().map(invocation.source());
        for (final Map.Entry<Type, Permission> entry : accessMap.entrySet()) {
            if (GenericTypeReflector.isSuperType(entry.getKey(), cloudSender.getClass())) {
                if (this.manager.testPermission(cloudSender, entry.getValue()).allowed()) {
                    return true;
                }
            }
        }
        return false;
    }

    private @Nullable CommandNode<C> namedNode() {
        return this.manager.commandTree().getNamedNode(this.command.name());
    }
}
