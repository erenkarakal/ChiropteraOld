package me.eren.chiroptera.handlers.server;

import com.google.common.eventbus.Subscribe;
import me.eren.chiroptera.Chiroptera;
import me.eren.chiroptera.ChiropteraServer;
import me.eren.chiroptera.events.PacketReceivedEvent;
import me.eren.chiroptera.events.server.ClientDisconnectEvent;
import me.eren.chiroptera.packets.DisconnectPacket;

import java.io.IOException;

public class ServerDisconnectHandler {
    @Subscribe
    public void handleClientDisconnect(PacketReceivedEvent e) {
        if (e.getPacket() instanceof DisconnectPacket) {
            try {
                ChiropteraServer.authenticatedClients.get(e.getClientIdentifier()).close();
                ChiropteraServer.authenticatedClients.remove(e.getClientIdentifier());
                Chiroptera.getLog().info("Client " + e.getClientIdentifier() + " disconnected.");

                ClientDisconnectEvent event = new ClientDisconnectEvent(e.getClientIdentifier(), false);
                Chiroptera.getEventBus().post(event);
            } catch (IOException ignored) {}
        }
    }
}
