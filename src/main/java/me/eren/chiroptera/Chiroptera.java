package me.eren.chiroptera;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

public final class Chiroptera extends JavaPlugin {

    private static Logger logger;
    private static Chiroptera instance;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        logger.info("Enabling Chiroptera " + getDescription().getVersion());

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
                InetSocketAddress address = new InetSocketAddress(host, port);
                ChiropteraClient.connect(address, capacity, secret);
            }
        });

    }

    @Override
    public void onDisable() {
        ChiropteraServer.shouldListen = false;
        ChiropteraClient.shouldDisconnect = true;
        logger.info("Disabling Chiroptera " + getDescription().getVersion());
    }

    public static Logger getLog() {
        return logger;
    }

    public static Chiroptera getInstance() {
        return instance;
    }

}
