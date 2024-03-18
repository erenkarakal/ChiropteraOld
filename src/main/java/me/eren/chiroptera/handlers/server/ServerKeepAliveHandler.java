package me.eren.chiroptera.handlers.server;

import me.eren.chiroptera.Chiroptera;
import me.eren.chiroptera.ChiropteraServer;
import me.eren.chiroptera.events.PacketRecievedEvent;
import me.eren.chiroptera.events.server.ClientDisconnectEvent;
import me.eren.chiroptera.handlers.client.ClientKeepAliveHandler;
import me.eren.chiroptera.packets.KeepAlivePacket;
import me.eren.chiroptera.packets.KickPacket;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ServerKeepAliveHandler {

    private static BukkitTask bukkitTask = null;
    private static byte currentByte = (byte) 0;
    public static final List<String> respondedClients = new ArrayList<>();

    /**
     * Starts sending keep alive packets every 20 seconds.
     */
    public static void startKeepingAlive() {
        KeepAlivePacket initialKeepAlivePacket = new KeepAlivePacket((byte) 0);
        ChiropteraServer.broadcast(initialKeepAlivePacket);

        bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(Chiroptera.getInstance(), () -> {

            kickDeadClients();
            currentByte = (byte) new Random().nextInt(255);
            KeepAlivePacket keepAlivePacket = new KeepAlivePacket(currentByte);
            ChiropteraServer.broadcast(keepAlivePacket);

        },
        100L, // start the task 5 seconds later to wait for initial packet response
        500L); // 20 seconds + 5 more seconds to wait for response
    }

    public static void stopKeepingAlive() {
        if (bukkitTask == null) return;
        bukkitTask.cancel();
        bukkitTask = null;
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
                Bukkit.getScheduler().runTask(Chiroptera.getInstance(), () -> {
                    ClientDisconnectEvent event = new ClientDisconnectEvent(entry.getKey(), true);
                    Bukkit.getPluginManager().callEvent(event);
                });
                entry.getValue().close();
            } catch (IOException ignored) {}
        }
    }

    public static class KeepAliveListener implements Listener {
        @EventHandler
        public void onKeepAlivePacket(PacketRecievedEvent e) {
            if (e.getPacket() instanceof KeepAlivePacket keepAlivePacket &&
                    keepAlivePacket.getRandom() == currentByte) {
                respondedClients.add(e.getClientIdentifier());
            }
        }
    }

}
