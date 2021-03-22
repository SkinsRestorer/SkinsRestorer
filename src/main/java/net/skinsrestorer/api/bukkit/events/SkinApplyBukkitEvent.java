package net.skinsrestorer.api.bukkit.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class SkinApplyBukkitEvent extends PlayerEvent implements Cancellable {
    public static final HandlerList HANDLERS = new HandlerList();
    private boolean isCancelled = false;
    private final Object props;

    public SkinApplyBukkitEvent(@NotNull Player who, Object props) {
        super(who);
        this.props = props;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    public Object getProps() {
        return props;
    }
}
