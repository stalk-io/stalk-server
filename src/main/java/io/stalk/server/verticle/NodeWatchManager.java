package io.stalk.server.verticle;

import io.stalk.common.server.zk.ZooKeeperClient;
import io.stalk.common.server.zk.ZooKeeperConnectionException;
import io.stalk.common.server.zk.ZooKeeperUtils;
import io.stalk.server.MonitorServer;
import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.*;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

public class NodeWatchManager extends AbstractVerticle{


    private EventBus        eb;
    private ZooKeeperClient zkClient;
    private boolean 		isWatching;
    private boolean			isCreated;
    private String 			rootPath;

    @Override
    void start(JsonObject appConfig) {

        eb = vertx.eventBus();
        JsonArray   zookeeperServers 	= appConfig.getArray("zookeeper-address", null);
        int 	    zookeeperTimeout 	= appConfig.getInteger("zookeeper-timeout", 5000);
        this.rootPath = appConfig.getString("zookeeper-rootPath", "/STALK/node");

        try {
            zkClient = new ZooKeeperClient(zookeeperTimeout, ZooKeeperClient.Credentials.NONE, zookeeperServers);
        } catch (ZooKeeperConnectionException e) {
            ERROR("zookeeper is not existed [%s]", zookeeperServers.encode());
            e.printStackTrace();
        }
    }

    @Override
    public void handle(Message<JsonObject> message) {

        String action = message.body().getString("action");

        if("create".equals(action)){

            if(isCreated){

                sendOK(message);

            }else{

                try {
                    createNode(
                            message.body().getString("channel"),
                            message.body().getObject("data")
                    );

                    isCreated = true;

                    // OK
                    sendOK(message);

                } catch (ZooKeeperConnectionException
                        | InterruptedException
                        | KeeperException e) {
                    e.printStackTrace();

                    // ERROR
                    sendError(message, e.getMessage());

                }
            }

        }else if("watch".equals(action)){

            if(!isWatching){
                watching();
                isWatching = false;
            }
            sendOK(message);

        }else if("delete".equals(action)){
            String channel = message.body().getString("channel");
            if(StringUtils.isNotBlank(channel)){
                try {
                    zkClient.get().delete(rootPath+"/"+channel, -1);
                    sendOK(message);
                } catch (InterruptedException | KeeperException
                        | ZooKeeperConnectionException e) {
                    sendError(message, e.getMessage());
                }

            }else{
                sendError(message, "The name of channel is empty");
            }

        }else if("watchServerInfo".equals(action)){

            if(!isWatching){
                watchServerInfo();
                isWatching = false;
            }
            sendOK(message);

        }

    }

    @Override
    public void stop() {
        try {
            super.stop();
            zkClient.close();
        } catch (Exception e) {
            e.printStackTrace();
            ERROR(e.getMessage());
        }
    }

    private void createNode(final String channel, final JsonObject data) throws ZooKeeperConnectionException, InterruptedException, KeeperException{

        ZooKeeperUtils.ensurePath(zkClient, ZooDefs.Ids.OPEN_ACL_UNSAFE, rootPath);

        if (zkClient.get().exists(rootPath+"/"+channel, false) == null) {

            DEBUG("create node [%s]", data.encode());

            zkClient.get().create(
                    rootPath+"/"+channel,
                    data.encode().getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT);
        }

    }


    private void watching(){

        try {

            ZooKeeperUtils.ensurePath(zkClient, ZooDefs.Ids.OPEN_ACL_UNSAFE, rootPath);

            List<String> channels = zkClient.get().getChildren(rootPath, new Watcher() {
                public void process(WatchedEvent event) {
                    try {
                        List<String> channels = zkClient.get().getChildren(rootPath, this);
                        DEBUG("** WATCHED ** %s %s", rootPath, channels);
                        refreshNode(channels);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            if(channels.size() > 0){

                refreshNode(channels);

            }else{

                ERROR(" message server is not existed from [%s]..", rootPath);

            }

        } catch (KeeperException | InterruptedException
                | ZooKeeperConnectionException e) {
            e.printStackTrace();
            ERROR("%s", e.getMessage());
        }
    }


    private void watchServerInfo(){

        try {

            ZooKeeperUtils.ensurePath(zkClient, ZooDefs.Ids.OPEN_ACL_UNSAFE, rootPath);

            List<String> channels = zkClient.get().getChildren(rootPath, new Watcher() {
                public void process(WatchedEvent event) {
                    try {
                        List<String> channels = zkClient.get().getChildren(rootPath, this);
                        DEBUG("** WATCHED ** %s %s", rootPath, channels);

                        sendServerInfo(channels);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            if(channels.size() > 0){

                sendServerInfo(channels);

            }else{

                ERROR(" message server is not existed from [%s]..", rootPath);

            }

        } catch (KeeperException | InterruptedException
                | ZooKeeperConnectionException e) {
            e.printStackTrace();
            ERROR("%s", e.getMessage());
        }
    }

    private void sendServerInfo(List<String> channels) throws KeeperException, InterruptedException, ZooKeeperConnectionException{

        JsonObject result = new JsonObject();
        result.putNumber("count", channels.size());

        if(channels.size() > 0){

            JsonArray infos = new JsonArray();
            for(String channel : channels){

                JsonObject nodes = new JsonObject(
                        new String(zkClient.get().getData(rootPath + "/"+channel, false, null))
                );

                JsonObject info = new JsonObject();
                info.putString("channel", channel);
                info.putObject("server", nodes.getObject("server"));
                infos.addObject(info);

            }

            result.putArray("servers", infos);
        }

        eb.send(MonitorServer.class.getName(), result);
    }

    private void refreshNode(List<String> channels) throws KeeperException, InterruptedException, ZooKeeperConnectionException{

        if(channels.size() > 0){

            JsonArray servers = new JsonArray();
            JsonArray redises = new JsonArray();

            for(String channel : channels){

                JsonObject nodes = new JsonObject(
                        new String(zkClient.get().getData(rootPath + "/"+channel, false, null))
                );

                servers.addObject(nodes.getObject("server").putString("channel", channel));
                redises.addObject(nodes.getObject("redis").putString("channel", channel));

            }

            eb.publish(SessionManager.class.getName() , new JsonObject()
                    .putString("action", "refresh")
                    .putArray("channels", servers)
            );

            eb.publish(PublishManager.class.getName() , new JsonObject()
                    .putString("action", "refresh")
                    .putArray("channels", redises)
            );

        }else{

            ERROR("message server is not existed from [%s]", rootPath);

            eb.publish(SessionManager.class.getName(),
                    new JsonObject().putString("action", "destory"));

            eb.publish(PublishManager.class.getName(),
                    new JsonObject().putString("action", "destory"));

        }

    }
}
