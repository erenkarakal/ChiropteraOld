package me.eren.chiroptera.events.server;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a client disconnects or times out. This event is not called when the client is kicked.
 */
public class ClientDisconnectEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final String identifier;
    private final boolean isTimedOut;

    public ClientDisconnectEvent(String identifier, boolean isTimedOut) {
        this.identifier = identifier;
        this.isTimedOut = isTimedOut;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public boolean isTimedOut() {
        return this.isTimedOut;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

}
