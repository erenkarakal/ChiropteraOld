package me.eren.chiroptera.events;

import me.eren.chiroptera.Packet;

public class PacketReceivedEvent {
    private final Packet packet;
    private final String clientIdentifier;

    public PacketReceivedEvent(Packet packet, String clientIdentifier) {
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
}
