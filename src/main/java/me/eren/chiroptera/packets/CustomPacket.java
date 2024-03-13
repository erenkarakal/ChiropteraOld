package me.eren.chiroptera.packets;

import me.eren.chiroptera.Packet;

import java.io.Serializable;
import java.util.Map;

public class CustomPacket extends Packet {
    public CustomPacket(byte id, Map<Integer, Serializable> data) {
        super(id, data);
    }
}
