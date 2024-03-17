package me.eren.chiroptera;

import me.eren.chiroptera.handlers.client.ClientKeepAliveHandler;
import me.eren.chiroptera.handlers.client.ClientKickHandler;
import me.eren.chiroptera.handlers.server.ServerDisconnectHandler;
import me.eren.chiroptera.handlers.server.ServerKeepAliveHandler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public final class Chiroptera extends JavaPlugin {

    private static Logger logger;
    private static Chiroptera instance;
    private static boolean isServer;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();

        saveDefaultConfig();

        isServer = getConfig().getString("type", "server").equals("server");
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
            if (isServer) {
                Bukkit.getPluginManager().registerEvents(new ServerKeepAliveHandler.KeepAliveListener(), this);
                Bukkit.getPluginManager().registerEvents(new ServerDisconnectHandler(), this);
                logger.info("Starting a listener on port " + port + "...");
                List<String> whitelistedIps = getConfig().getStringList("whitelisted-ips");
                if (!whitelistedIps.isEmpty()) ChiropteraServer.whitelistedIps.addAll(whitelistedIps);
                ChiropteraServer.listen(port, capacity, secret);

            } else {
                Bukkit.getPluginManager().registerEvents(new ClientKeepAliveHandler.KeepAliveListener(), this);
                Bukkit.getPluginManager().registerEvents(new ClientKickHandler(), this);
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

    /**
     * @return true if the current server is a ChiropteraServer
     */
    public static boolean isServer() { return isServer; }

}
