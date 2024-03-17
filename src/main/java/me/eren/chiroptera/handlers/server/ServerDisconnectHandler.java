package me.eren.chiroptera.handlers.server;

import me.eren.chiroptera.Chiroptera;
import me.eren.chiroptera.ChiropteraServer;
import me.eren.chiroptera.events.PacketRecievedEvent;
import me.eren.chiroptera.packets.DisconnectPacket;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.IOException;

public class ServerDisconnectHandler implements Listener {
    @EventHandler
    public void onClientDisconnect(PacketRecievedEvent e) {
        if (e.getPacket() instanceof DisconnectPacket disconnectPacket) {
            try {
                ChiropteraServer.authenticatedClients.get(e.getClientIdentifier()).close();
                ChiropteraServer.authenticatedClients.remove(e.getClientIdentifier());
                Chiroptera.getLog().info("Client " + e.getClientIdentifier() + " disconnected.");
            } catch (IOException ignored) {}
        }
    }
}
