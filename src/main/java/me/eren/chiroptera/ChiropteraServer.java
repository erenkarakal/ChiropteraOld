package me.eren.chiroptera;

import me.eren.chiroptera.events.PacketReceivedEvent;
import me.eren.chiroptera.events.server.ClientConnectEvent;
import me.eren.chiroptera.handlers.server.ServerKeepAliveHandler;
import me.eren.chiroptera.packets.AuthenticatePacket;
import me.eren.chiroptera.packets.KickPacket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ChiropteraServer {

    private static boolean isListening;
    protected static boolean shouldListen = true;
    public static final Map<String, SocketChannel> authenticatedClients = new HashMap<>();
    public static final List<String> whitelistedIps = new ArrayList<>();

    public static void listen(int port, int capacity, String secret) {
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
            ServerKeepAliveHandler.startKeepingAlive();

            while (shouldListen) {
                selector.selectedKeys().clear();
                selector.select();

                for (SelectionKey key : selector.selectedKeys()) {
                    if (key.isAcceptable()) {
                        // accept a new connection
                        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                        SocketChannel clientChannel = serverChannel.accept();
                        if (!whitelistedIps.isEmpty()) { // check if ip whitelist is enabled and client is whitelisted
                            String ip = getFormattedAddress(clientChannel).split(":")[0];
                            if (!whitelistedIps.contains(ip)) {
                                KickPacket kickPacket = new KickPacket("Not whitelisted.");
                                sendPacket(clientChannel, kickPacket);
                                clientChannel.close();
                                return;
                            }
                        }
                        if (clientChannel != null) {
                            clientChannel.configureBlocking(false);
                            clientChannel.register(selector, SelectionKey.OP_READ);
                            Chiroptera.getLog().info("A client connected! (" + getFormattedAddress(clientChannel) + ")");
                        } else {
                            Chiroptera.getLog().warning("Got null client? This is not good.");
                        }

                        Chiroptera.getScheduler().schedule(() -> {
                            if (!authenticatedClients.containsValue(clientChannel)) {
                                try {
                                    // client did not authenticate in time, kick them.
                                    if (clientChannel != null) {
                                        Chiroptera.getLog().info("A client was kicked because they didn't authenticate in time. (" + getFormattedAddress(clientChannel) + ")");
                                        KickPacket kickPacket = new KickPacket("Did not authenticate in time.");
                                        sendPacket(clientChannel, kickPacket);
                                        clientChannel.close();
                                    }
                                } catch (IOException e) {
                                    Chiroptera.getLog().warning("Error while kicking a client. " + e.getMessage());
                                }
                            }
                        }, 5, TimeUnit.SECONDS);

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
                        // authenticate the client
                        if (!authenticatedClients.containsValue(clientChannel) && packet instanceof AuthenticatePacket authenticatePacket) {
                            String loginIdentifier = authenticatePacket.getIdentifier();
                            String loginSecret = authenticatePacket.getSecret();

                            // if it already exists, don't kick the other client
                            if (authenticatedClients.containsKey(loginIdentifier)) {
                                KickPacket kickPacket = new KickPacket("Client with this identifier already exists.");
                                sendPacket(clientChannel, kickPacket);
                                clientChannel.close();
                                continue;
                            }

                            if (secret.equals(loginSecret)) {
                                authenticatedClients.put(loginIdentifier, clientChannel);
                                Chiroptera.getLog().info("A client named " + loginIdentifier + " authenticated! (" + getFormattedAddress(clientChannel) + ")");
                                ServerKeepAliveHandler.respondedClients.add(loginIdentifier); // to avoid false kicks due to connecting on a bad time.
                                ClientConnectEvent event = new ClientConnectEvent(loginIdentifier);
                                Chiroptera.getEventBus().post(event);
                            } else {
                                Chiroptera.getLog().info("A client named " + loginIdentifier + " was disconnected for wrong secret. (" + getFormattedAddress(clientChannel) + ")");
                                clientChannel.close();
                            }
                        } else { // the client is already authenticated. process the data
                            PacketReceivedEvent event = new PacketReceivedEvent(packet, getClientIdentifier(clientChannel));
                            Chiroptera.getEventBus().post(event);
                        }
                    }
                }
            }
        } catch (IOException e) {
            if (e instanceof ClosedChannelException || e instanceof SocketException) return;
            Chiroptera.getLog().warning("Got an error while starting/running the server. " + e.getMessage());
            e.printStackTrace(System.out);
        } finally {
            close();
        }
    }

    public static void close() {
        if (!isListening) return;

        isListening = false;
        shouldListen = false;
        KickPacket kickPacket = new KickPacket("Server closed.");
        broadcast(kickPacket);
        authenticatedClients.clear();
        ServerKeepAliveHandler.stopKeepingAlive();
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
     * @param packet Packet to send.
     * @return true if the packet was successfully sent.
     */
    public static boolean sendPacket(String clientIdentifier, Packet packet) {
        if (!isListening) return false;

        SocketChannel client = authenticatedClients.get(clientIdentifier);
        if (client == null) return false;

        return sendPacket(client, packet);
    }

    private static boolean sendPacket(SocketChannel client, Packet packet) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(packet.serialize());
            while (buffer.hasRemaining()) {
                client.write(buffer);
            }
            return true;
        } catch (IOException e) {
            return false;
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

    public static String getClientIdentifier(SocketChannel clientChannel) {
        for (Map.Entry<String, SocketChannel> entry : authenticatedClients.entrySet()) {
            if (entry.getValue().equals(clientChannel)) return entry.getKey();
        }
        return "unknown";
    }


}
