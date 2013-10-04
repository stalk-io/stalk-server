package io.stalk.server.verticle;

import org.apache.commons.lang.StringUtils;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public class SubscribeThread implements Runnable {


    protected EventBus eb;
    private String channel;

    private String host;
    private int port;

    private String replyAddress;

    private Jedis 	jedis;

    private Logger log;

    public SubscribeThread(EventBus eb, String host, int port, String channel, String replyAddress){
        this(null, eb, host, port, channel, replyAddress);
    }

    public SubscribeThread(Logger log, EventBus eb, String host, int port, String channel, String replyAddress){
        this.log = log;
        this.eb = eb;
        this.channel = channel;

        this.replyAddress = replyAddress;

        this.host = host;
        this.port = port;

    }

    @Override
    public void run() {

        if( StringUtils.isEmpty(host) ){
            this.jedis = new Jedis("localhost");
        }else{
            this.jedis = new Jedis(host, port);
        }

        jedis.subscribe( new JedisPubSub() {

            @Override
            public void onMessage(String channel, String message) {
                eb.send(replyAddress,
                        new JsonObject(message).putString("action", "message")
                );
            }

            @Override
            public void onSubscribe(String channel, int subscribedChannels) {

                JsonObject json = new JsonObject()
                        .putString("channel", channel)
                        .putString("action"	, "subscribe");

                eb.publish(replyAddress, json);
            }

            @Override
            public void onUnsubscribe(String channel, int subscribedChannels) {

                JsonObject json = new JsonObject()
                        .putString("channel", channel)
                        .putString("action"	, "unsubscribe");

                eb.publish(replyAddress, json);
            }

            @Override
            public void onPMessage(String pattern, String channel,String message) {
            }

            @Override
            public void onPUnsubscribe(String pattern, int subscribedChannels) {
            }

            @Override
            public void onPSubscribe(String pattern, int subscribedChannels) {

            }

        }, channel);
    }

}
