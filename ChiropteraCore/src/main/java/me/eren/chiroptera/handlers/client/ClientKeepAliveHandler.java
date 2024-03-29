package me.eren.chiroptera.handlers.client;

import com.google.common.eventbus.Subscribe;
import me.eren.chiroptera.Chiroptera;
import me.eren.chiroptera.ChiropteraClient;
import me.eren.chiroptera.events.PacketReceivedEvent;
import me.eren.chiroptera.packets.KeepAlivePacket;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ClientKeepAliveHandler {

    private static ScheduledFuture<?> keepAliveTask;
    private static long TIMEOUT_TIME = 0;

    /**
     * Checks if the server sent a keep alive packet within 20 seconds, every 20 seconds.
     * If not, disconnect from the server.
     */
    public static void start() {
        keepAliveTask = Chiroptera.getScheduler().scheduleWithFixedDelay(() -> {
                    if (System.currentTimeMillis() > TIMEOUT_TIME) {
                        Chiroptera.getLogger().warning("Server timed out.");
                        ChiropteraClient.shouldDisconnect = true;
                    }
                },
                20, // start 20 seconds later
                20, // every 20 seconds
                TimeUnit.SECONDS);
    }

    public static void stop() {
        if (keepAliveTask == null) return;
        keepAliveTask.cancel(true);
        keepAliveTask = null;
    }

    // respond to the server with the same packet
    public static class KeepAliveListener {
        @Subscribe
        public void handleKeepAlivePacket(PacketReceivedEvent e) {
            if (e.packet() instanceof KeepAlivePacket keepAlivePacket) {
                ChiropteraClient.sendPacket(keepAlivePacket);
                long newTimeout = System.currentTimeMillis() + 25_000; // 25 seconds
                if (newTimeout > TIMEOUT_TIME) {
                    TIMEOUT_TIME = newTimeout;
                }
            }
        }
    }
}
