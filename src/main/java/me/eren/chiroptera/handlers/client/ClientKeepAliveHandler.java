package me.eren.chiroptera.handlers.client;

import me.eren.chiroptera.Chiroptera;
import me.eren.chiroptera.ChiropteraClient;
import me.eren.chiroptera.events.PacketRecievedEvent;
import me.eren.chiroptera.packets.KeepAlivePacket;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

public class ClientKeepAliveHandler {

    private static BukkitTask bukkitTask = null;
    private static long TIMEOUT_TIME = 0;

    /**
     * Checks if the server sent a keep alive packet within 20 seconds, every 20 seconds.
     * If not, disconnect from the server.
     */
    public static void start() {
        bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(Chiroptera.getInstance(), () -> {
            if (System.currentTimeMillis() > TIMEOUT_TIME) {
                Chiroptera.getLog().warning("Server timed out.");
                ChiropteraClient.disconnect();
            }
        },
        400L, // start 20 seconds later
        400L); // loop every 20 seconds
    }

    public static void stop() {
        if (bukkitTask == null) return;
        bukkitTask.cancel();
        bukkitTask = null;
    }

    // respond to the server with the same packet
    public static class KeepAliveListener implements Listener {
        @EventHandler
        public void onKeepAlivePacket(PacketRecievedEvent e) {
            if (e.getPacket() instanceof KeepAlivePacket keepAlivePacket) {
                ChiropteraClient.sendPacket(keepAlivePacket);
                long newTimeout = System.currentTimeMillis() + 21_000;
                if (newTimeout > TIMEOUT_TIME) {
                    TIMEOUT_TIME = newTimeout;
                }
            }
        }
    }
}
