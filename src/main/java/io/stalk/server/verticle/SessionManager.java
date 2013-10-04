package io.stalk.server.verticle;

import io.stalk.common.server.NodeManager;
import io.stalk.common.server.ServerNodeManager;
import io.stalk.common.server.node.ServerNode;
import org.apache.commons.lang.StringUtils;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class SessionManager extends AbstractVerticle{

    private NodeManager<ServerNode> serverNodeManager;

    private JedisPool redisPool;

    public void start(JsonObject appConfig) {


        // 1. REDIS (SESSION) connect!!
        JsonObject sessionConfig = appConfig.getObject("redis-address");

        if(sessionConfig != null){
            JedisPoolConfig config = new JedisPoolConfig();
            config.testOnBorrow = true;

            String host = sessionConfig.getString("host", "localhost");
            int port 	= sessionConfig.getInteger("port", 6379).intValue();

            JedisPool jedisPool;
            if( StringUtils.isEmpty(host) ){
                jedisPool = new JedisPool(config, "localhost");
            }else{
                jedisPool = new JedisPool(config, host, port);
            }

            INFO(" > session storage CONNECTED - " + host + ":" + port);

            this.redisPool = jedisPool;
        }

        // 2. ServerNodeManager init!!
        serverNodeManager = new ServerNodeManager();

    }

    @Override
    public void stop() {
        try {
            super.stop();
            this.redisPool.destroy();
            if(serverNodeManager != null) 	serverNodeManager.destoryNode();
        } catch (Exception e) {
        }
    }

    @Override
    public void handle(Message<JsonObject> message) {

        String action = message.body().getString("action");

        if("in".equals(action) && this.redisPool != null){

            Jedis       jedis 	    = this.redisPool.getResource();
            ServerNode  serverNode  = null;

            try {

                String refer = message.body().getString("refer");


                String[] refers = getHostUrl(refer);
                String key 		= refers[0]; // domain name > 'stalk.io'
                String field 	= refers[1]; // uri path    > '/chat'

                String channelAndCount = jedis.hget(key, field);


                // not existed in session redis.
                if(channelAndCount == null){

                    serverNode = serverNodeManager.getNode(refer);

                    if(serverNode == null){
                        sendError(message, "server node is not existed.");
                        return;
                    }

                    DEBUG(" [in] (Not Existed in Session Storage) - %s - %s:%s"
                            , serverNode.getChannel(), key, field);

                    jedis.hset(key, field, serverNode.getChannel()+"^0"); // init channel and count '0'

                }else{

                    String 	channel	= channelAndCount.substring(0, channelAndCount.indexOf("^"));
                    //int 	count  	= Integer.parseInt(channelAndCount.substring(channelAndCount.indexOf("^")+1));

                    serverNode = serverNodeManager.getNodeByKey(channel);

                    if(serverNode == null){ // when the target server is crushed!!

                        // delete session info.
                        jedis.hdel(key, field);

                        serverNode = serverNodeManager.getNode(refer);

                        DEBUG(" [in] (CRUSHED!!! retry to get new Node) - %s - %s:%s"
                                , serverNode.getChannel(), key, field);

                        if(serverNode != null) jedis.hset(key, field, serverNode.getChannel()+"^0"); //channel and count '0'

                    }

                    DEBUG(" ACTION[in] (Existed) - %s", serverNode.getChannel());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                this.redisPool.returnResource(jedis);
            }

            if(serverNode != null){
                JsonObject json = new JsonObject();
                json.putString("channel", 	serverNode.getChannel());
                json.putString("host", 		serverNode.getHost());
                json.putNumber("port", 		serverNode.getPort());

                sendOK(message, json);
            }else{
                sendError(message, "server node is not existed.");
            }

        }else if("update".equals(action)){

            String  refer   = message.body().getString("refer");
            int     count   = message.body().getInteger("count").intValue();

            String[] refers = getHostUrl(refer);

            Jedis   jedis   = this.redisPool.getResource();

            try {
                if(count > 0){

                    String channel = message.body().getString("channel");
                    jedis.hset(refers[0], refers[1], channel+"^"+count);

                }else{

                    jedis.hdel(refers[0], refers[1]);

                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                this.redisPool.returnResource(jedis);
            }

            sendOK(message);

        }else if("refresh".equals(action)){

            JsonArray channels = message.body().getArray("channels");
            if(channels != null){
                serverNodeManager.refreshNode(channels);
            }

        }else if("destory".equals(action)){
            serverNodeManager.destoryNode();
        }

    }




    private String[] getHostUrl(String str){

        String[] rtn = new String[2];

        if(str.indexOf("https://") >= 0){
            if(str.substring(8).indexOf("/") >= 0){
                int s =  8+str.substring(8).indexOf("/");
                rtn[0] = str.substring(0, s);
                rtn[1] = str.substring(s);
            }else{
                rtn[0] =  str;
                rtn[1] = "/";
            }
        } else if(str.indexOf("http://") >= 0){
            if(str.substring(7).indexOf("/") >= 0){
                int s =  7+str.substring(7).indexOf("/");
                rtn[0] = str.substring(0, s);
                rtn[1] = str.substring(s);
            }else{
                rtn[0] = str;
                rtn[1] = "/";
            }
        } else {
            if(str.indexOf("/") >= 0){
                rtn[0] = str.substring(0, str.indexOf("/"));
                rtn[1] = str.substring(str.indexOf("/"));
            }else{
                rtn[0] = str;
                rtn[1] = "/";
            }
        }

        return rtn;

    }


}
