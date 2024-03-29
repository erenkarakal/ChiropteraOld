package me.eren.chiroptera.events;

import me.eren.chiroptera.Packet;

public record PacketReceivedEvent(Packet packet, String clientIdentifier) {

    /**
     * @return The packet that was sent.
     */
    @Override
    public Packet packet() {
        return this.packet;
    }

    /**
     * @return The identifier of the client that sent this packet, null if it was sent by the server.
     */
    @Override
    public String clientIdentifier() {
        return this.clientIdentifier;
    }
}
