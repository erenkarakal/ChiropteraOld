package me.eren.chiroptera;

import me.eren.chiroptera.events.PacketRecievedEvent;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;

public class ChiropteraClient {

    private static boolean isConnected = false;
    protected static volatile boolean shouldDisconnect = false;
    private static SocketChannel server = null;

    protected static void connect(InetSocketAddress address, int capacity, String secret, String identifier) {
        if (isConnected) return;

        try (SocketChannel tempServer = SocketChannel.open()) {
            server = tempServer;
            server.connect(address);
            isConnected = server.isConnected();

            // send the authentication packet
            Packet authenticatePacket = new Packet((byte) 0, Map.of(0, identifier, 1, secret));
            sendPacket(authenticatePacket);

            // read the server's response
            ByteBuffer buffer = ByteBuffer.allocate(capacity);

            while (!shouldDisconnect) {
                int bytesRead = server.read(buffer);
                if (bytesRead <= 0) continue; // no data to read
                buffer.flip();
                if (!buffer.hasRemaining()) continue; // we don't want empty data

                byte[] dataBytes = new byte[bytesRead];
                buffer.get(dataBytes);

                Packet packet = Packet.deserialize(dataBytes);

                Bukkit.getScheduler().runTask(Chiroptera.getInstance(), () -> {
                    PacketRecievedEvent event = new PacketRecievedEvent(packet);
                    Bukkit.getPluginManager().callEvent(event);
                });

                buffer.clear(); // clear for the next read
            }

        } catch (IOException e) {
            throw new RuntimeException("Error while connecting/listening to the server.", e);
        }
    }

    /**
     * Sends a packet to the connected server.
     * @param packet Packet to send
     */
    public static void sendPacket(Packet packet) {
        if (!isConnected || server == null) return;

        try {
            byte[] serializedPacket = packet.serialize();
            ByteBuffer buffer = ByteBuffer.wrap(serializedPacket);
            while (buffer.hasRemaining()) {
                server.write(buffer);
            }
        } catch (IOException e) {
            Chiroptera.getLog().warning("Error while sending a packet. " + e.getMessage());
        }
    }

}
