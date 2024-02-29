package me.eren.chiroptera;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;

public class ChiropteraServer {

    private static boolean isListening = false;
    protected static boolean shouldListen = true;
    private static final Set<SocketChannel> authenticatedClients = new HashSet<>();

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
                        clientChannel.configureBlocking(false);
                        clientChannel.register(selector, SelectionKey.OP_READ);
                        Chiroptera.getLog().info("A client connected! (" + getFormattedAddress(clientChannel) + ")");

                    } else if (key.isReadable()) {
                        // read data from a connected client
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(capacity);
                        int bytesRead = clientChannel.read(buffer);
                        if (bytesRead <= 0) continue; // there is no data to read.

                        buffer.flip();
                        String data = new String(buffer.array(), 0, bytesRead).trim();

                        // authenticate the client
                        if (!authenticatedClients.contains(clientChannel)) {
                            if (data.equals(secret)) {
                                authenticatedClients.add(clientChannel);
                                Chiroptera.getLog().info("A client authenticated! (" + getFormattedAddress(clientChannel) + ")");

                            } else {
                                Chiroptera.getLog().info("A client was disconnected for wrong secret. (" + getFormattedAddress(clientChannel) + ")");
                                clientChannel.close();
                            }
                        } else { // the client is authenticated.
                            Chiroptera.getLog().info("received data: " + data);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while starting the server", e);
        }
    }

    /**
     * Broadcasts a message to all authenticated clients.
     * @param message Message to broadcast
     */
    private static void broadcastMessage(String message) {
        try {
            ByteBuffer messageBuffer = ByteBuffer.wrap(message.getBytes());

            for (SocketChannel channel : authenticatedClients) {
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
