package org.incendo.cloud.velocity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.SenderMapperHolder;
import org.incendo.cloud.brigadier.suggestion.TooltipSuggestion;
import org.incendo.cloud.caption.CaptionProvider;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.suggestion.SuggestionFactory;
import org.incendo.cloud.velocity.parser.PlayerParser;
import org.incendo.cloud.velocity.parser.ServerParser;

@Singleton
public class SRVelocityCommandManager<C> extends CommandManager<C> implements SenderMapperHolder<CommandSource, C> {

    /**
     * Default caption for {@link VelocityCaptionKeys#ARGUMENT_PARSE_FAILURE_PLAYER}
     */
    public static final String ARGUMENT_PARSE_FAILURE_PLAYER = "'<input>' is not a valid player";

    /**
     * Default caption for {@link VelocityCaptionKeys#ARGUMENT_PARSE_FAILURE_SERVER}
     */
    public static final String ARGUMENT_PARSE_FAILURE_SERVER = "'<input>' is not a valid server";

    private final ProxyServer proxyServer;
    private final SenderMapper<CommandSource, C> senderMapper;
    private final SuggestionFactory<C, ? extends TooltipSuggestion> suggestionFactory;

    /**
     * Create a new command manager instance
     *
     * @param plugin                       Container for the owning plugin
     * @param proxyServer                  ProxyServer instance
     * @param commandExecutionCoordinator  Coordinator provider
     * @param senderMapper                 Function that maps {@link CommandSource} to the command sender type
     */
    @Inject
    @SuppressWarnings({"this-escape"})
    public SRVelocityCommandManager(
            final @NonNull PluginContainer plugin,
            final @NonNull ProxyServer proxyServer,
            final @NonNull ExecutionCoordinator<C> commandExecutionCoordinator,
            final @NonNull SenderMapper<CommandSource, C> senderMapper
    ) {
        super(commandExecutionCoordinator, new SRVelocityPluginRegistrationHandler<>());
        this.proxyServer = proxyServer;
        this.senderMapper = senderMapper;
        this.suggestionFactory = super.suggestionFactory().mapped(TooltipSuggestion::tooltipSuggestion);

        ((SRVelocityPluginRegistrationHandler<C>) this.commandRegistrationHandler()).initialize(this);

        /* Register Velocity Parsers */
        this.parserRegistry()
                .registerParser(PlayerParser.playerParser())
                .registerParser(ServerParser.serverParser());

        /* Register default captions */
        this.captionRegistry()
                .registerProvider(CaptionProvider.<C>constantProvider()
                        .putCaption(VelocityCaptionKeys.ARGUMENT_PARSE_FAILURE_PLAYER, ARGUMENT_PARSE_FAILURE_PLAYER)
                        .putCaption(VelocityCaptionKeys.ARGUMENT_PARSE_FAILURE_SERVER, ARGUMENT_PARSE_FAILURE_SERVER)
                        .build());

        proxyServer.getEventManager().register(plugin, ServerPreConnectEvent.class, ev -> {
            this.lockRegistration();
        });
        this.parameterInjectorRegistry().registerInjector(
                CommandSource.class,
                (context, annotations) -> this.senderMapper.reverse(context.sender())
        );

        this.registerDefaultExceptionHandlers();
    }

    @Override
    public final boolean hasPermission(final @NonNull C sender, final @NonNull String permission) {
        return this.senderMapper.reverse(sender).hasPermission(permission);
    }

    @Override
    public final @NonNull SuggestionFactory<C, ? extends TooltipSuggestion> suggestionFactory() {
        return this.suggestionFactory;
    }

    final @NonNull ProxyServer proxyServer() {
        return this.proxyServer;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private void registerDefaultExceptionHandlers() {
        this.registerDefaultExceptionHandlers(
                triplet -> {
                    final CommandSource source = triplet.first().inject(CommandSource.class).orElseThrow(NullPointerException::new);
                    final String message = triplet.first().formatCaption(triplet.second(), triplet.third());
                    source.sendMessage(Component.text(message, NamedTextColor.RED));
                },
                pair -> pair.second().printStackTrace()
        );
    }

    public final @NonNull SenderMapper<CommandSource, C> senderMapper() {
        return this.senderMapper;
    }
}

