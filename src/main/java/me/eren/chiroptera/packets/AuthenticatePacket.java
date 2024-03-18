package me.eren.chiroptera.packets;

import me.eren.chiroptera.Packet;

import java.util.Map;

/**
 * Sent by the client as soon as they connect, if the client doesn't send this packet within 5 seconds, they get kicked.
 */
public class AuthenticatePacket extends Packet {

    private final String identifier;
    private final String secret;

    public AuthenticatePacket(String identifier, String secret) {
        super((byte) -1, Map.of(0, identifier, 1, secret));
        this.identifier = identifier;
        this.secret = secret;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public String getSecret() {
        return this.secret;
    }

}
