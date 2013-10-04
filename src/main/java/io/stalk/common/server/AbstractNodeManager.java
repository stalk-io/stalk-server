package io.stalk.common.server;

import io.stalk.common.server.map.NodeMap;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

public abstract class AbstractNodeManager<X> implements NodeManager<X> {

    protected NodeMap<X> nodes;
    protected Logger log;
    protected String prefix = "REDIS";

    public AbstractNodeManager() {
        nodes = initNodeMap();
    }

    protected void activeLogger(Logger logger) {
        this.log = logger;
    }

    protected void activeLogger(Logger logger, String prefix) {
        this.log = logger;
        this.prefix = prefix;
    }

    protected abstract NodeMap<X> initNodeMap();

    @Override
    public X getNode(String keyOrRefer) {
        return nodes.get(keyOrRefer);
    }

    @Override
    public X getNodeByKey(String key) {
        return getNode(key);
    }


    protected void DEBUG(String message, Object... args) {
        if (log != null) log.debug("[MOD::NODE(" + prefix + ")] " + String.format(message, args));
    }

    protected void ERROR(String message, Object... args) {
        if (log != null) log.error("[MOD::NODE(" + prefix + ")] " + String.format(message, args));
    }

    protected void sendOK(Message<JsonObject> message) {
        sendOK(message, null);
    }

    protected void sendStatus(String status, Message<JsonObject> message) {
        sendStatus(status, message, null);
    }

    protected void sendStatus(String status, Message<JsonObject> message, JsonObject json) {
        if (json == null) {
            json = new JsonObject();
        }
        json.putString("status", status);
        message.reply(json);
    }

    protected void sendOK(Message<JsonObject> message, JsonObject json) {
        sendStatus("ok", message, json);
    }

    protected void sendError(Message<JsonObject> message, String error) {
        sendError(message, error, null);
    }

    protected void sendError(Message<JsonObject> message, String error, Exception e) {

        JsonObject json = new JsonObject().putString("status", "error").putString("message", error);
        message.reply(json);
    }
}
