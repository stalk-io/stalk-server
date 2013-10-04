package io.stalk.common.server;

import io.stalk.common.server.ServerNodeManager.SERVER;
import io.stalk.common.server.map.NodeMap;
import io.stalk.common.server.map.NodeRegistry;
import io.stalk.common.server.node.RedisPoolNode;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.HashMap;

public class RedisNodeManager extends AbstractNodeManager<RedisPoolNode> {

    private boolean isOk = false;

    public RedisNodeManager() {
        super();
    }

    public RedisNodeManager(Logger logger) {
        super();
        if (logger != null) activeLogger(logger);
    }

    public RedisNodeManager(Logger logger, String prefix) {
        super();
        if (logger != null) activeLogger(logger, prefix);
    }

    @Override
    protected NodeMap<RedisPoolNode> initNodeMap() {
        return new NodeRegistry<RedisPoolNode>();
    }

    @Override
    public void refreshNode(JsonArray jsonArray) {

        HashMap<String, Boolean> channelMap = new HashMap<String, Boolean>();

        for (String channel : nodes.getKeys()) {
            channelMap.put(channel, false);
        }

        for (Object serverInfo : jsonArray) {

            JsonObject serverConf = (JsonObject) serverInfo;

            String channel = serverConf.getString("channel");
            channelMap.put(channel, true);

            if (!nodes.isExist(channel)) {
                RedisPoolNode node = new RedisPoolNode(serverConf, log, prefix);
                nodes.add(node.getChannel(), node);
            }

        }

        for (String channel : channelMap.keySet()) {
            if (!channelMap.get(channel)) {
                nodes.remove(channel);
            }

        }

        DEBUG("refreshNode - size : %d ", nodes.getKeys().size());

        if (nodes.getKeys().size() > 0) {
            isOk = true;
        } else {
            isOk = false;
        }
    }

    @Override
    public void messageHandle(Message<JsonObject> message) {

        String action = message.body().getString("action");
        DEBUG("messageHandle : %s ", message.body());

        // to be deleted!!!
        switch (action) {
            case "message:publish":

                sendOK(message, publish(
                        message.body().getString("channel"),
                        message.body()
                ));

                break;

            case SERVER.OK:
                sendOK(message, new JsonObject().putBoolean("ok", isOk));
                break;

            default:
                sendError(message, "[REDIS] Invalid action: " + action);
                return;
        }

    }


    private JsonObject publish(String channel, JsonObject message) {

        RedisPoolNode redisNode = nodes.get(channel);
        long result = redisNode.publish(channel, message.encode());

        JsonObject json = new JsonObject();
        json.putNumber("result", result);

        DEBUG("publish : [channel:%s] / result : %s", channel, json);

        return json;

    }

    @Override
    public void destoryNode() {
        for (String channel : nodes.getKeys()) {
            isOk = false;
            nodes.remove(channel);
        }
    }

}
