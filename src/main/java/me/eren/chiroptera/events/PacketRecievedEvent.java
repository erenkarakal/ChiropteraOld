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
    private final String clientIdentifier;

    public PacketRecievedEvent(Packet packet, String clientIdentifier) {
        this.packet = packet;
        this.clientIdentifier = clientIdentifier;
    }

    /**
     * @return The packet that was sent.
     */
    public Packet getPacket() {
        return this.packet;
    }

    /**
     * @return The identifier of the client that sent this packet, null if it was sent by the server.
     */
    public String getClientIdentifier() {
        return this.clientIdentifier;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

}
