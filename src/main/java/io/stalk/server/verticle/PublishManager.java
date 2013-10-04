package io.stalk.server.verticle;

import io.stalk.common.server.NodeManager;
import io.stalk.common.server.RedisNodeManager;
import io.stalk.common.server.node.RedisPoolNode;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class PublishManager extends AbstractVerticle{


    private NodeManager<RedisPoolNode> redisNodeManager;

    @Override
    void start(JsonObject appConfig) {
        redisNodeManager = new RedisNodeManager();
    }

    @Override
    public void stop() {
        try {
            super.stop();
            if(redisNodeManager != null) 	redisNodeManager.destoryNode();
        } catch (Exception e) {
        }
    }

    @Override
    public void handle(Message<JsonObject> message) {
        String action = message.body().getString("action");

        if("refresh".equals(action)){

            JsonArray channels = message.body().getArray("channels");

            if(channels != null){
                redisNodeManager.refreshNode(channels);
            }

        }else if("destory".equals(action)){
            redisNodeManager.destoryNode();

        }else if("pub".equals(action)){

            String channel = message.body().getString("channel");
            RedisPoolNode redisNode = redisNodeManager.getNode(channel);
            long result = redisNode.publish(channel, message.body().encode());

            JsonObject json = new JsonObject();
            json.putNumber("result", 	result);

            sendOK(message, json);
        }

    }
}
