package me.eren.chiroptera;

import com.google.common.eventbus.EventBus;
import me.eren.chiroptera.handlers.client.ClientKeepAliveHandler;
import me.eren.chiroptera.handlers.client.ClientKickHandler;
import me.eren.chiroptera.handlers.server.ServerDisconnectHandler;
import me.eren.chiroptera.handlers.server.ServerForwardPacketHandler;
import me.eren.chiroptera.handlers.server.ServerKeepAliveHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

public class Chiroptera {

    private static final Logger logger = Logger.getLogger("Chiroptera");
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static final EventBus EVENT_BUS = new EventBus();

    static {
        // server
        EVENT_BUS.register(new ServerKeepAliveHandler.KeepAliveListener());
        EVENT_BUS.register(new ServerDisconnectHandler());
        EVENT_BUS.register(new ServerForwardPacketHandler());

        // client
        EVENT_BUS.register(new ClientKeepAliveHandler.KeepAliveListener());
        EVENT_BUS.register(new ClientKickHandler());
    }

    public static Logger getLogger() {
        return logger;
    }

    public static ScheduledExecutorService getScheduler() {
        return SCHEDULER;
    }

    public static EventBus getEventBus() {
        return EVENT_BUS;
    }

}
