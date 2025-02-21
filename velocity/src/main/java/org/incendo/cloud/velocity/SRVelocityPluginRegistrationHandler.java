package org.incendo.cloud.velocity;

import com.velocitypowered.api.command.CommandMeta;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.internal.CommandRegistrationHandler;

import java.util.Collection;

final class SRVelocityPluginRegistrationHandler<C> implements CommandRegistrationHandler<C> {

    private SRVelocityCommandManager<C> manager;

    void initialize(final @NonNull SRVelocityCommandManager<C> velocityCommandManager) {
        this.manager = velocityCommandManager;
    }

    @Override
    public boolean registerCommand(final @NonNull Command<C> command) {
        final CommandComponent<C> component = command.rootComponent();
        final Collection<String> aliases = component.alternativeAliases();
        final CommandMeta commandMeta = this.manager.proxyServer().getCommandManager()
                .metaBuilder(command.rootComponent().name())
                .aliases(aliases.toArray(new String[0])).build();
        aliases.forEach(this.manager.proxyServer().getCommandManager()::unregister);
        this.manager.proxyServer().getCommandManager().register(commandMeta, new SRVelocityCommand<>(command.rootComponent(), this.manager));
        return true;
    }
}
