package me.eren.chiroptera.events.server;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a client authenticates.
 */
public class ClientConnectEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final String identifier;

    public ClientConnectEvent(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

}
