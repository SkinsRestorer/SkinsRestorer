package org.incendo.cloud.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.brigadier.suggestion.TooltipSuggestion;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionFactory;
import org.incendo.cloud.suggestion.Suggestions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SRVelocityCommandTest {
    private static final String ARGUMENTS = "set st";
    private static final String COMMAND_LINE = "skin " + ARGUMENTS;

    private final CompletableFuture<Suggestions<Object, TooltipSuggestion>> pending = new CompletableFuture<>();
    private final ExecutorService eventLoop = Executors.newSingleThreadExecutor();
    private final CommandSource source = mock(CommandSource.class);
    private final RawCommand.Invocation invocation = mock(RawCommand.Invocation.class);

    private SRVelocityCommand<Object> command;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        SuggestionFactory<Object, TooltipSuggestion> factory = mock(SuggestionFactory.class);
        when(factory.suggest(source, COMMAND_LINE)).thenReturn(pending);
        // Keeps the real join() of the default method, so a blocking adapter times out instead of getting null.
        doCallRealMethod().when(factory).suggestImmediately(source, COMMAND_LINE);

        SRVelocityCommandManager<Object> manager = mock(SRVelocityCommandManager.class);
        doReturn(factory).when(manager).suggestionFactory();
        doReturn(SenderMapper.create(sender -> sender, sender -> (CommandSource) sender)).when(manager).senderMapper();

        CommandComponent<Object> component = mock(CommandComponent.class);
        when(component.name()).thenReturn("skin");

        when(invocation.source()).thenReturn(source);
        when(invocation.arguments()).thenReturn(ARGUMENTS);

        command = new SRVelocityCommand<>(component, manager);
    }

    @AfterEach
    void tearDown() {
        // Releases the event loop thread if the adapter under test waits on the future.
        pending.cancel(true);
        eventLoop.shutdownNow();
    }

    @Test
    void suggestAsyncDoesNotWaitForSuggestions() throws Exception {
        CompletableFuture<List<String>> result = eventLoop.submit(() -> command.suggestAsync(invocation))
                .get(5, TimeUnit.SECONDS);

        // A task queued behind the call runs, so the event loop is free while suggestions are pending.
        eventLoop.submit(() -> {
        }).get(5, TimeUnit.SECONDS);
        assertFalse(result.isDone());

        pending.complete(suggestions("set steve", "set stone", "other notch"));

        assertEquals(List.of("steve", "stone"), result.get(5, TimeUnit.SECONDS));
    }

    @Test
    void suggestAsyncPropagatesFailureThroughFuture() throws Exception {
        CompletableFuture<List<String>> result = eventLoop.submit(() -> command.suggestAsync(invocation))
                .get(5, TimeUnit.SECONDS);

        IllegalStateException failure = new IllegalStateException("suggestion failure");
        pending.completeExceptionally(failure);

        ExecutionException thrown = assertThrows(ExecutionException.class, () -> result.get(5, TimeUnit.SECONDS));
        assertSame(failure, thrown.getCause());
    }

    @Test
    void suggestMatchesSuggestAsync() {
        pending.complete(suggestions("set steve", "other notch"));

        assertEquals(List.of("steve"), command.suggest(invocation));
    }

    @SuppressWarnings("unchecked")
    private Suggestions<Object, TooltipSuggestion> suggestions(String... values) {
        List<TooltipSuggestion> list = Arrays.stream(values)
                .map(Suggestion::suggestion)
                .map(TooltipSuggestion::tooltipSuggestion)
                .toList();
        return Suggestions.create(mock(CommandContext.class), list, CommandInput.of(ARGUMENTS));
    }
}
