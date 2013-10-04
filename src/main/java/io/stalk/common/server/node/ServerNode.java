package io.stalk.common.server.node;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

public class ServerNode extends AbstractNode {

    // @ TODO to Map!!!
    private String channel;

    private String host;
    private int port;

    public ServerNode(String host, int port, String channel, Logger log) {
        super(log, "SERVER");
        this.host = host;
        this.port = port;
        this.channel = channel;
    }

    public ServerNode(JsonObject jsonObject, Logger log) {
        super(log, "SERVER");
        this.channel = jsonObject.getString("channel");
        this.host = jsonObject.getString("host");
        this.port = jsonObject.getNumber("port").intValue();

        DEBUG("created %s", jsonObject);
    }

    public String getHost() {
        return host;
    }


    public void setHost(String host) {
        this.host = host;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }


}
