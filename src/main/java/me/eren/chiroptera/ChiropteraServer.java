package me.eren.chiroptera;

import me.eren.chiroptera.events.PacketRecievedEvent;
import me.eren.chiroptera.events.server.ClientConnectEvent;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class ChiropteraServer {

    private static boolean isListening = false;
    protected volatile static boolean shouldListen = true;
    private static final Map<String, SocketChannel> authenticatedClients = new HashMap<>();

    protected static void listen(int port, int capacity, String secret) {
        if (isListening) return;

        try {
            // initialize
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
            serverSocketChannel.configureBlocking(false);

            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            isListening = true;
            Chiroptera.getLog().info("Listening on port " + port);

            while (shouldListen) {
                selector.selectedKeys().clear();
                selector.select();

                for (SelectionKey key : selector.selectedKeys()) {
                    if (key.isAcceptable()) {
                        // accept a new connection
                        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                        SocketChannel clientChannel = serverChannel.accept();
                        if (clientChannel != null) {
                            clientChannel.configureBlocking(false);
                            clientChannel.register(selector, SelectionKey.OP_READ);
                            Chiroptera.getLog().info("A client connected! (" + getFormattedAddress(clientChannel) + ")");
                        } else {
                            Chiroptera.getLog().warning("Got null client? This is not good.");
                        }

                        Bukkit.getScheduler().runTaskLaterAsynchronously(Chiroptera.getInstance(), () -> {
                            if (!authenticatedClients.containsValue(clientChannel)) {
                                try {
                                    // client did not authenticate in time, kick them.
                                    if (clientChannel != null) {
                                        Chiroptera.getLog().info("A client was kicked because they didn't authenticate in time. (" + getFormattedAddress(clientChannel) + ")");
                                        clientChannel.close();
                                    }
                                } catch (IOException e) {
                                    Chiroptera.getLog().warning("Error while kicking a client. " + e.getMessage());
                                }
                            }
                        }, 100L); // 5 seconds

                    } else if (key.isReadable()) {
                        // read data from a connected client
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(capacity);
                        int bytesRead = clientChannel.read(buffer);
                        if (bytesRead <= 0) continue; // there is no data to read.
                        buffer.flip();
                        if (!buffer.hasRemaining()) continue; // we don't want empty data

                        byte[] dataBytes = new byte[bytesRead];
                        buffer.get(dataBytes);

                        Packet packet = Packet.deserialize(dataBytes);
                        Chiroptera.getLog().info("packet: " + packet);
                        // authenticate the client
                        if (!authenticatedClients.containsValue(clientChannel) && packet.id() == 0) {
                            String loginIdentifier = (String) packet.data().get(0);
                            String loginSecret = (String) packet.data().get(1);

                            // if it already exists, don't kick the other client
                            if (authenticatedClients.containsKey(loginIdentifier)) {
                                clientChannel.close();
                                continue;
                            }

                            if (secret.equals(loginSecret)) {
                                authenticatedClients.put(loginIdentifier, clientChannel);
                                Chiroptera.getLog().info("A client named " + loginIdentifier + " authenticated! (" + getFormattedAddress(clientChannel) + ")");
                                Bukkit.getScheduler().runTask(Chiroptera.getInstance(), () -> {
                                    ClientConnectEvent event = new ClientConnectEvent(loginIdentifier);
                                    Bukkit.getPluginManager().callEvent(event);
                                });
                            } else {
                                Chiroptera.getLog().info("A client named " + loginIdentifier + " was disconnected for wrong secret. (" + getFormattedAddress(clientChannel) + ")");
                                clientChannel.close();
                            }
                        } else { // the client is already authenticated. process the data
                            Bukkit.getScheduler().runTask(Chiroptera.getInstance(), () -> {
                                PacketRecievedEvent event = new PacketRecievedEvent(packet);
                                Bukkit.getPluginManager().callEvent(event);
                            });
                        }
                    }
                }
            }
        } catch (IOException e) {
            if (e instanceof SocketException) return; // a client disconnected, no need for an exception
            throw new RuntimeException("Error while starting/running the server", e);
        }
    }

    /**
     * Broadcasts a packet to all authenticated clients.
     * @param packet Packet to broadcast
     */
    public static void broadcast(Packet packet) {
        for (String clientID : authenticatedClients.keySet()) {
            sendPacket(clientID, packet);
        }
    }

    /**
     * Sends a packet to a specific client. The client must be authenticated.
     * @param clientIdentifier The ID of the client.
     * @param packet Packet to send
     */
    public static void sendPacket(String clientIdentifier, Packet packet) {
        if (!isListening) return;

        SocketChannel client = authenticatedClients.get(clientIdentifier);
        if (client == null) return;

        try {
            ByteBuffer buffer = ByteBuffer.wrap(packet.serialize());
            while (buffer.hasRemaining()) {
                client.write(buffer);
            }
        } catch (IOException e) {
            Chiroptera.getLog().warning("Error while sending data to client. " + e.getMessage());
        }
    }

    private static String getFormattedAddress(SocketChannel clientChannel) {
        try {
            InetSocketAddress clientAddress = (InetSocketAddress) clientChannel.getRemoteAddress();
            String clientIp = clientAddress.getAddress().getHostAddress();
            int clientPort = clientAddress.getPort();
            return clientIp + ":" + clientPort;
        } catch (IOException e) {
            return "unknown";
        }
    }

    private static String getClientIdentifier(SocketChannel clientChannel) {
        for (Map.Entry<String, SocketChannel> entry : authenticatedClients.entrySet()) {
            if (entry.getValue().equals(clientChannel)) return entry.getKey();
        }
        return "unknown";
    }

}
