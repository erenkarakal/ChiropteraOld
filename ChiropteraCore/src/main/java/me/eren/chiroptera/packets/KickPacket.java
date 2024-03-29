package me.eren.chiroptera.packets;

import me.eren.chiroptera.Packet;

import java.util.Map;

/**
 * Sent by the server when it kicks a client for whatever reason. Client uses this to log the kick reason and properly disconnect.
 */
public class KickPacket extends Packet {

    private final String reason;

    public KickPacket(String reason) {
        super((byte) -4, Map.of(0, reason));
        this.reason = reason;
    }

    public String getReason() {
        return this.reason;
    }
    
}
