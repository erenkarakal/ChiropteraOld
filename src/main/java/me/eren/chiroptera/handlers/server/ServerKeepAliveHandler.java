package me.eren.chiroptera.handlers.server;

import com.google.common.eventbus.Subscribe;
import me.eren.chiroptera.Chiroptera;
import me.eren.chiroptera.ChiropteraServer;
import me.eren.chiroptera.events.PacketReceivedEvent;
import me.eren.chiroptera.events.server.ClientDisconnectEvent;
import me.eren.chiroptera.packets.KeepAlivePacket;
import me.eren.chiroptera.packets.KickPacket;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ServerKeepAliveHandler {

    private static ScheduledFuture<?> keepAliveTask = null;
    private static byte currentByte = (byte) 0;
    public static final List<String> respondedClients = new ArrayList<>();

    /**
     * Starts sending keep alive packets every 20 seconds.
     */
    public static void startKeepingAlive() {
        KeepAlivePacket initialKeepAlivePacket = new KeepAlivePacket((byte) 0);
        ChiropteraServer.broadcast(initialKeepAlivePacket);

        keepAliveTask = Chiroptera.getScheduler().scheduleWithFixedDelay(() -> {
            kickDeadClients();
            currentByte = (byte) new Random().nextInt(255);
            KeepAlivePacket keepAlivePacket = new KeepAlivePacket(currentByte);
            ChiropteraServer.broadcast(keepAlivePacket);

        },
        5, // start the task 5 seconds later to wait for initial packet response
        25, // 20 seconds keep alive + 5 more seconds to wait for response
        TimeUnit.SECONDS);
    }

    public static void stopKeepingAlive() {
        if (keepAliveTask == null) return;
        keepAliveTask.cancel(true);
        keepAliveTask = null;
    }

    private static void kickDeadClients() {
        KickPacket kickPacket = new KickPacket("Timed out.");
        for (Map.Entry<String, SocketChannel> entry : ChiropteraServer.authenticatedClients.entrySet()) {
            if (respondedClients.contains(entry.getKey())) {
                respondedClients.remove(entry.getKey());
                continue;
            }
            try {
                ChiropteraServer.sendPacket(entry.getKey(), kickPacket);
                ClientDisconnectEvent event = new ClientDisconnectEvent(entry.getKey(), true);
                Chiroptera.getEventBus().post(event);
                entry.getValue().close();
            } catch (IOException ignored) {}
        }
    }

    public static class KeepAliveListener {
        @Subscribe
        public void handleKeepAlivePacket(PacketReceivedEvent e) {
            if (e.getPacket() instanceof KeepAlivePacket keepAlivePacket &&
                    keepAlivePacket.getRandom() == currentByte) {
                respondedClients.add(e.getClientIdentifier());
            }
        }
    }

}
