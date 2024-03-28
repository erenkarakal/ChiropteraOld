package me.eren.chiroptera.handlers.server;

import com.google.common.eventbus.Subscribe;
import me.eren.chiroptera.ChiropteraServer;
import me.eren.chiroptera.events.PacketReceivedEvent;
import me.eren.chiroptera.packets.ForwardPacket;

public class ServerForwardPacketHandler {
    @Subscribe
    public void handleForwardPacket(PacketReceivedEvent e) {
        if (e.getPacket() instanceof ForwardPacket packet) {
            ChiropteraServer.sendPacket(packet.getClientToSend(), packet.getPacketToSend());
        }
    }
}
