package me.eren.chiroptera.packets;

import me.eren.chiroptera.Packet;

import java.io.Serializable;
import java.util.Map;

/**
 * A custom packet with no set ID or data.
 */
public class CustomPacket extends Packet {

    public CustomPacket(byte id, Map<Integer, Serializable> data) {
        super(id, data);
    }

}
