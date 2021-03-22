package net.skinsrestorer.api.bungeecord.events;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;
import org.jetbrains.annotations.NotNull;

public class SkinApplyBungeeEvent extends Event implements Cancellable {
    private boolean isCancelled = false;
    private final ProxiedPlayer who;
    private String nick;

    public SkinApplyBungeeEvent(@NotNull ProxiedPlayer who, String nick) {
        this.who = who;
        this.nick = nick;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    public ProxiedPlayer getPlayer() {
        return who;
    }

    public String getNick() {
        return nick;
    }

    @SuppressWarnings("unused")
    public void setNick(String nick) {
        this.nick = nick;
    }
}
