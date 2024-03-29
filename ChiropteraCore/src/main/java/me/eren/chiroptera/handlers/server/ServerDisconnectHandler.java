package me.eren.chiroptera.handlers.server;

import com.google.common.eventbus.Subscribe;
import me.eren.chiroptera.Chiroptera;
import me.eren.chiroptera.ChiropteraServer;
import me.eren.chiroptera.packets.DisconnectPacket;
import me.eren.chiroptera.events.PacketReceivedEvent;
import me.eren.chiroptera.events.server.ClientDisconnectEvent;

import java.io.IOException;

public class ServerDisconnectHandler {
    @Subscribe
    public void handleClientDisconnect(PacketReceivedEvent e) {
        if (e.packet() instanceof DisconnectPacket) {
            try {
                ChiropteraServer.authenticatedClients.get(e.clientIdentifier()).close();
                ChiropteraServer.authenticatedClients.remove(e.clientIdentifier());
                Chiroptera.getLogger().info("Client " + e.clientIdentifier() + " disconnected.");

                ClientDisconnectEvent event = new ClientDisconnectEvent(e.clientIdentifier(), false);
                Chiroptera.getEventBus().post(event);
            } catch (IOException ignored) {}
        }
    }
}
