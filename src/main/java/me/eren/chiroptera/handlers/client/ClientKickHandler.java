package me.eren.chiroptera.handlers.client;

import me.eren.chiroptera.Chiroptera;
import me.eren.chiroptera.ChiropteraClient;
import me.eren.chiroptera.events.PacketRecievedEvent;
import me.eren.chiroptera.packets.KickPacket;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ClientKickHandler implements Listener {
    @EventHandler
    public void onKickPacket(PacketRecievedEvent e) {
        if (e.getPacket() instanceof KickPacket kickPacket) {
            ChiropteraClient.disconnect();
            Chiroptera.getLog().warning("Got kicked by the server. Reason: " + kickPacket.getReason());
        }
    }
}
