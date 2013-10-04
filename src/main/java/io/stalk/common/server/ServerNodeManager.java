package io.stalk.common.server;

import io.stalk.common.server.map.ConsistentHash;
import io.stalk.common.server.map.NodeMap;
import io.stalk.common.server.node.ServerNode;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.HashMap;

public class ServerNodeManager extends AbstractNodeManager<ServerNode> {

    interface SERVER {

        String NODE = "server:node";
        String NODE_BY_CHANNEL = "server:nodeByChannel";
        String OK = "server:Ok";

    }

    private boolean isOk = false;
    private HashMap<String, ServerNode> serverList = new HashMap<String, ServerNode>();


    public ServerNodeManager() {
        super();
    }

    public ServerNodeManager(Logger logger) {
        super();
        if (logger != null) activeLogger(logger, "SERVER");
    }

    @Override
    protected NodeMap<ServerNode> initNodeMap() {
        return new ConsistentHash<ServerNode>();
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
                ServerNode node = new ServerNode(serverConf, log);
                nodes.add(node.getChannel(), node);
                serverList.put(node.getChannel(), node);
            }

        }

        for (String channel : channelMap.keySet()) {
            if (!channelMap.get(channel)) {
                nodes.remove(channel);
                serverList.remove(channel);
            }

        }

        if (nodes.getKeys().size() > 0) {
            isOk = true;
        } else {
            isOk = false;
        }

        DEBUG("nodes is refreshed (size:%d)", nodes.getKeys().size());

    }

    @Override
    public void destoryNode() {
        for (String channel : nodes.getKeys()) {
            isOk = false;
            nodes.remove(channel);
            serverList.remove(channel);
        }
    }

	/*public void send(String action, JsonObject jsonData){
        jsonData.putString(Module.Config.ACTION, action);
		eventbus.send(address, jsonData);
	}*/

    @Override
    public void messageHandle(Message<JsonObject> message) {

        String action = message.body().getString("action");

        DEBUG("messageHandle : %s ", message.body());

        switch (action) {
            case SERVER.NODE:
                String refer = message.body().getString("refer");
                sendOK(message,
                        getServerNode(refer)
                                .putString("key", message.body().getString("key"))
                                .putString("field", message.body().getString("field"))
                );
                break;
            case SERVER.OK:
                sendOK(message, new JsonObject().putBoolean("ok", isOk));
                break;
            default:
                sendError(message, "[SERVER] Invalid action: " + action);
                return;
        }

    }


    public JsonObject getServerNode(String refer) {

        ServerNode serverNode = getNode(refer);

        JsonObject json = new JsonObject();
        json.putString("channel", serverNode.getChannel());
        json.putString("host", serverNode.getHost());
        json.putNumber("port", serverNode.getPort());

        DEBUG("getServerNode : %s ", json);

        return json;
    }

    @Override
    public ServerNode getNodeByKey(String channel) {
        ServerNode serverNode = serverList.get(channel);
        return serverNode;
    }

}
