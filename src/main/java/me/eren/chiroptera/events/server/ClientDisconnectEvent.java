package me.eren.chiroptera.events.server;

public class ClientDisconnectEvent {
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
}
