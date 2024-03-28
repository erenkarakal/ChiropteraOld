package me.eren.chiroptera;

import me.eren.chiroptera.events.PacketReceivedEvent;
import me.eren.chiroptera.handlers.client.ClientKeepAliveHandler;
import me.eren.chiroptera.packets.AuthenticatePacket;
import me.eren.chiroptera.packets.DisconnectPacket;
import me.eren.chiroptera.packets.ForwardPacket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

public class ChiropteraClient {

    protected static boolean shouldDisconnect = false;
    private static SocketChannel server = null;
    private static int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 15;

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
            reconnectAttempts = 0;

            while (!shouldDisconnect && server != null && server.isConnected()) {
                int bytesRead = server.read(buffer);
                if (bytesRead <= 0) continue; // no data to read
                buffer.flip();
                if (!buffer.hasRemaining()) continue; // we don't want empty data

                byte[] dataBytes = new byte[bytesRead];
                buffer.get(dataBytes);

                Packet packet = Packet.deserialize(dataBytes);
                PacketReceivedEvent event = new PacketReceivedEvent(packet, null);
                Chiroptera.getEventBus().post(event);

                buffer.clear(); // clear for the next read
            }

        } catch (IOException e) {
            if (shouldDisconnect) return;

            if (reconnectAttempts == 0) {
                Chiroptera.getLog().warning("Connection failed, will try 15 more times with a 10 second interval...");
            }

            if (reconnectAttempts++ < MAX_RECONNECT_ATTEMPTS) {
                Chiroptera.getScheduler().schedule(() -> connect(address, capacity, secret, identifier), 10, TimeUnit.SECONDS);
                return;
            }

            Chiroptera.getLog().warning("Error while connecting to the server. " + e.getMessage());
            disconnect();
        }
    }

    public static void disconnect() {
        if (server == null) return;

        try {
            if (server.isConnected()) {
                DisconnectPacket disconnectPacket = new DisconnectPacket();
                sendPacket(disconnectPacket);
            }
            ClientKeepAliveHandler.stop();
            server.close();
            shouldDisconnect = true;
            Chiroptera.getLog().info("Disconnected.");
        } catch (IOException e) {
            Chiroptera.getLog().warning("Error while disconnecting. " + e.getMessage());
        }
    }

    /**
     * Sends a packet to the connected server.
     * @param packet Packet to send
     * @return true if the packet was successfully sent.
     */
    public static boolean sendPacket(Packet packet) {
        if (server == null) return false;

        try {
            byte[] serializedPacket = packet.serialize();
            ByteBuffer buffer = ByteBuffer.wrap(serializedPacket);
            while (buffer.hasRemaining()) {
                server.write(buffer);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Uses the built-in Forward Packet to forward a packet to a specific client.
     * @param clientIdentifier The ID of the client.
     * @param packet Packet to send.
     * @return true if the packet was successfully sent.
     */
    public static boolean sendPacket(String clientIdentifier, Packet packet) {
        ForwardPacket forwardPacket = new ForwardPacket(clientIdentifier, packet);
        return sendPacket(forwardPacket);
    }

}
