package me.eren.chiroptera.handlers.client;

import com.google.common.eventbus.Subscribe;
import me.eren.chiroptera.Chiroptera;
import me.eren.chiroptera.ChiropteraClient;
import me.eren.chiroptera.events.PacketReceivedEvent;
import me.eren.chiroptera.packets.KickPacket;

public class ClientKickHandler {
    @Subscribe
    public void handleKickPacket(PacketReceivedEvent e) {
        if (e.packet() instanceof KickPacket kickPacket) {
            ChiropteraClient.disconnect();
            Chiroptera.getLogger().warning("Got kicked by the server. Reason: " + kickPacket.getReason());
        }
    }
}
