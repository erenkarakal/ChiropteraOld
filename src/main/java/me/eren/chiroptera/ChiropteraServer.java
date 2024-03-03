package me.eren.chiroptera;

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
    protected static boolean shouldListen = true;
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
                    } else if (key.isReadable()) {
                        // read data from a connected client
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(capacity);
                        int bytesRead = clientChannel.read(buffer);
                        if (bytesRead <= 0) continue; // there is no data to read.

                        buffer.flip();
                        String data = new String(buffer.array(), 0, bytesRead).trim();
                        if (data.isEmpty()) continue; // we don't want empty data

                        // authenticate the client
                        if (!authenticatedClients.containsValue(clientChannel)) {
                            String[] loginData = data.split(" ");
                            if (loginData.length != 2) continue;

                            if (secret.equals(loginData[0])) { // secret
                                authenticatedClients.put(loginData[1], clientChannel); // identifier
                                Chiroptera.getLog().info("A client named " + loginData[1] + " authenticated! (" + getFormattedAddress(clientChannel) + ")");
                            } else {
                                Chiroptera.getLog().info("A client named " + loginData[1] + " was disconnected for wrong secret. (" + getFormattedAddress(clientChannel) + ")");
                                clientChannel.close();
                            }
                        } else { // the client is already authenticated. process the data
                            Chiroptera.getLog().info("received data: " + data);
                        }
                    }
                }
                selector.selectedKeys().clear();
            }
        } catch (IOException e) {
            if (e instanceof SocketException) { // remove all disconnected clients
                authenticatedClients.entrySet().removeIf(entry -> {
                    if (!entry.getValue().isConnected()) {
                        Chiroptera.getLog().warning("A client named " + entry.getKey() + " disconnected.");
                        return true;
                    }
                    return false;
                });
                return;
            }

            throw new RuntimeException("Error while starting the server", e);
        }
    }

    /**
     * Broadcasts a message to all authenticated clients.
     * @param message Message to broadcast
     */
    public static void broadcastMessage(String message) {
        try {
            ByteBuffer messageBuffer = ByteBuffer.wrap(message.getBytes());

            for (SocketChannel channel : authenticatedClients.values()) {
                channel.write(messageBuffer.duplicate());
                messageBuffer.rewind();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while broadcasting a message.", e);
        }
    }

    private static String getFormattedAddress(SocketChannel clientChannel) {
        try {
            InetSocketAddress clientAddress = (InetSocketAddress) clientChannel.getRemoteAddress();
            String clientIp = clientAddress.getAddress().getHostAddress();
            int clientPort = clientAddress.getPort();
            return clientIp + ":" + clientPort;
        } catch (IOException e) {
            throw new RuntimeException("Error while formatting address.", e);
        }
    }

}
