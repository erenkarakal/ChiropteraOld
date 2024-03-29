package me.eren.chiroptera.events.server;

public record ClientDisconnectEvent(String identifier, boolean isTimedOut) {
}
