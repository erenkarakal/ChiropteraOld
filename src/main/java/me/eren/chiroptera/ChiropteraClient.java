package me.eren.chiroptera;

import me.eren.chiroptera.events.PacketRecievedEvent;
import me.eren.chiroptera.handlers.client.ClientKeepAliveHandler;
import me.eren.chiroptera.packets.AuthenticatePacket;
import me.eren.chiroptera.packets.DisconnectPacket;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ChiropteraClient {

    protected static volatile boolean shouldDisconnect = false;
    private static SocketChannel server = null;

    public static void connect(InetSocketAddress address, int capacity, String secret, String identifier) {
        if (server != null && server.isConnected()) return;

        try {
            server = SocketChannel.open();
            server.connect(address);

            // send the authentication packet
            Packet authenticatePacket = new AuthenticatePacket(identifier, secret);
            sendPacket(authenticatePacket);

            // read the server's response
            ByteBuffer buffer = ByteBuffer.allocate(capacity);

            ClientKeepAliveHandler.start();

            while (!shouldDisconnect && server != null && server.isConnected()) {
                int bytesRead = server.read(buffer);
                if (bytesRead <= 0) continue; // no data to read
                buffer.flip();
                if (!buffer.hasRemaining()) continue; // we don't want empty data

                byte[] dataBytes = new byte[bytesRead];
                buffer.get(dataBytes);

                Packet packet = Packet.deserialize(dataBytes);

                Bukkit.getScheduler().runTask(Chiroptera.getInstance(), () -> {
                    PacketRecievedEvent event = new PacketRecievedEvent(packet, null);
                    Bukkit.getPluginManager().callEvent(event);
                });

                buffer.clear(); // clear for the next read
            }

        } catch (IOException e) {
            Chiroptera.getLog().warning("Error while connecting to the server. " + e.getMessage());

        } finally {
            disconnect();
        }
    }

    public static void disconnect() {
        if (server == null) return;

        try {
            shouldDisconnect = true;
            DisconnectPacket disconnectPacket = new DisconnectPacket();
            sendPacket(disconnectPacket);
            server.close();
            server = null;
            ClientKeepAliveHandler.stop();
            Chiroptera.getLog().info("Disconnected.");
        } catch (IOException e) {
            Chiroptera.getLog().warning("Error while disconnecting. " + e.getMessage());
        }
    }

    /**
     * Sends a packet to the connected server.
     * @param packet Packet to send
     */
    public static void sendPacket(Packet packet) {
        if (server == null) return;

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
