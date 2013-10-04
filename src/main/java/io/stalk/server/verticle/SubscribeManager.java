package io.stalk.server.verticle;

import io.stalk.server.MessageServer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public class SubscribeManager  extends AbstractVerticle{

    @Override
    void start(JsonObject appConfig) {
        JsonObject redisConf = appConfig.getObject("redis-address");

        new Thread(
                new SubscribeThread(
                    vertx.eventBus(),
                    redisConf.getString("host"),
                    redisConf.getInteger("port"),
                    appConfig.getString("channel"),
                    MessageServer.class.getName()
                )
        ).start();

    }

    @Override
    public void handle(Message<JsonObject> jsonObjectMessage) {
        // Do nothing...
    }
}
