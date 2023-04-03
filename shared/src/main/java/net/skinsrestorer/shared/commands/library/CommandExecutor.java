package net.skinsrestorer.shared.commands.library;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CommandExecutor<T> {
    private final CommandDispatcher<T> dispatcher;
    private final CommandPlatform<T> platformAdapter;

    public void execute(T executor, String input) {
        platformAdapter.runAsync(() -> {
            try {
                dispatcher.execute(input, executor);
            } catch (CommandSyntaxException e) {
                e.printStackTrace(); // TODO: Handle
            }
        });
    }

    public CompletableFuture<List<String>> tabComplete(T executor, String input) {
        return dispatcher.getCompletionSuggestions(dispatcher.parse(input, executor)).thenApply(suggestions ->
                suggestions.getList().stream().map(Suggestion::getText).collect(Collectors.toList()));
    }
}
