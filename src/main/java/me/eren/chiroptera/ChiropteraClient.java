package me.eren.chiroptera;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ChiropteraClient {

    private static boolean isConnected = false;
    protected static boolean shouldDisconnect = false;

    protected static void connect(InetSocketAddress address, int capacity, String secret) {
        if (isConnected) return;

        try {
            // connect to the server
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.connect(address);

            // send the secret to the server to authenticate
            socketChannel.write(ByteBuffer.wrap(secret.getBytes()));
            isConnected = true;

            // read the server's response
            ByteBuffer buffer = ByteBuffer.allocate(capacity);

            while (!shouldDisconnect) {
                int bytesRead = socketChannel.read(buffer);
                if (bytesRead <= 0) continue; // no data to read

                buffer.flip();
                String data = new String(buffer.array(), 0, bytesRead).trim();
                Chiroptera.getLog().info("received data: " + data);
                buffer.clear(); // clear for the next read
            }

        } catch (IOException e) {
            throw new RuntimeException("Error while connecting to the server.", e);
        }
    }

}
