package me.eren.chiroptera.events.server;

public class ClientConnectEvent {
    private final String identifier;

    public ClientConnectEvent(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return this.identifier;
    }
}
