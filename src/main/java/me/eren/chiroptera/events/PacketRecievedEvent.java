package me.eren.chiroptera.events;

import me.eren.chiroptera.Packet;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a packet is received by an authenticated client or the server.
 */
public class PacketRecievedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Packet packet;

    public PacketRecievedEvent(Packet packet) {
        this.packet = packet;
    }

    public Packet getPacket() {
        return this.packet;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

}
