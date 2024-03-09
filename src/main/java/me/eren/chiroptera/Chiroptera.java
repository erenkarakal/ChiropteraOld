package me.eren.chiroptera;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.logging.Logger;

public final class Chiroptera extends JavaPlugin {

    private static Logger logger;
    private static Chiroptera instance;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();

        saveDefaultConfig();

        String type = getConfig().getString("type", "server");
        String secret = getConfig().getString("secret", "123abc");
        int capacity = getConfig().getInt("capacity", 1024);
        int port = getConfig().getInt("port", 60_000);

        if (secret.equals("123abc")) {
            logger.warning("");
            logger.warning("Using default secret key!");
            logger.warning("Please change your secret in the config and restart the server. This is REALLY important.");
            logger.warning("");
        }

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            if (type.equals("server")) {
                ChiropteraServer.listen(port, capacity, secret);

            } else if (type.equals("client")) {
                String host = getConfig().getString("host", "0.0.0.0");
                String clientIdentifier = getConfig().getString("client-identifier", UUID.randomUUID().toString());
                InetSocketAddress address = new InetSocketAddress(host, port);
                ChiropteraClient.connect(address, capacity, secret, clientIdentifier);
            }
        });

    }

    @Override
    public void onDisable() {
        ChiropteraServer.shouldListen = false;
        ChiropteraClient.shouldDisconnect = true;
    }

    public static Logger getLog() {
        return logger;
    }

    public static Chiroptera getInstance() {
        return instance;
    }

}
