package io.stalk.server;

import io.netty.handler.codec.http.HttpHeaders;
import io.stalk.server.handler.SocketMessageHandler;
import io.stalk.server.verticle.NodeWatchManager;
import io.stalk.server.verticle.PublishManager;
import io.stalk.server.verticle.SessionManager;
import io.stalk.server.verticle.SubscribeManager;
import org.apache.commons.lang.StringUtils;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.sockjs.SockJSServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public class MessageServer extends Server{

    private EventBus eb;
    private ConcurrentMap<String, String> sessionStore;
    private String channel;

    @Override
    void start(final JsonObject appConfig, HttpServer server) {

        eb              = vertx.eventBus();
        channel         = appConfig.getString("channel");
        sessionStore    = vertx.sharedData().getMap("_REFER_STORAGE");

        List<Module> deployments = new ArrayList<Module>();
        deployments.add(new Module(SubscribeManager.class.getName() , appConfig, 1, false   ));
        deployments.add(new Module(NodeWatchManager.class.getName() , appConfig, 1, false   ));
        deployments.add(new Module(PublishManager.class.getName()                           ));
        deployments.add(new Module(SessionManager.class.getName()   , appConfig             ));

        deployWorkerVerticles(deployments, new Handler<Void>() {
            public void handle(Void event) {
                INFO(" *** modules are deployed. *** ");

                JsonObject createNodeAction = new JsonObject();
                createNodeAction.putString("action", "create");
                createNodeAction.putString("channel", channel);
                createNodeAction.putObject("data",
                        new JsonObject()
                                .putObject("server"	, new JsonObject()
                                        .putString("host", appConfig.getString("host"))
                                        .putNumber("port", appConfig.getNumber("port")))
                                .putObject("redis", appConfig.getObject("redis-address"))
                );

                eb.send(NodeWatchManager.class.getName(), createNodeAction, new Handler<Message<JsonObject>>() {
                    public void handle(Message<JsonObject> message) {
                        if("ok".equals(message.body().getString("status"))){
                            eb.send(NodeWatchManager.class.getName(), new JsonObject().putString("action", "watch"));
                        }
                    }
                });

            }
        });





        SockJSServer sockServer = vertx.createSockJSServer(server);
        sockServer.installApp(
                new JsonObject().putString("prefix", "/message"),
                new SocketMessageHandler(vertx, channel)
        );

    }

    @Override
    protected void postStart(JsonObject appConfig, HttpServer server) {

        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> message) {

                String action 	= message.body().getString("action");
                String socketId = message.body().getString("socketId");

                if("message".equals(action)){
                    message.body().putString("action", "LOGIN");
                    eb.send(socketId, new Buffer(message.body().encode()));
                }
            }
        };

        eb.registerHandler(MessageServer.class.getName(), myHandler, new AsyncResultHandler<Void>() {
            public void handle(AsyncResult<Void> asyncResult) {
                System.out.println("["+this.getClass().getName()+"] has been registered across the cluster ok? " + asyncResult.succeeded());
            }
        });

    }

    @Override
    public void stop() {
        try {
            super.stop();

            JsonObject delNode = new JsonObject();
            delNode.putString("action", "delete");
            delNode.putString("channel", channel);
            eb.send(NodeWatchManager.class.getName(), delNode);

            INFO(channel+" is closed !!! ");

        } catch (Exception e) {
        }
    }

    @Override
    public void handle(HttpServerRequest req) {

        if("/status/ping".equals(req.path())){


        }else if("/status/session/count".equals(req.path())){
            StringBuffer returnStr = new StringBuffer("");
            if(StringUtils.isEmpty(req.params().get("callback"))){
                returnStr.append("{\"count\":").append(sessionStore.size()).append("}");
            }else{
                returnStr
                        .append(req.params().get("callback"))
                        .append("(")
                        .append("{\"count\":").append(sessionStore.size()).append("}")
                        .append(");");
            }
            sendResponse(req, returnStr.toString());

        }else if (!req.path().startsWith("/message")){
            System.out.println(" *** bad request path *** "+req.path());
            req.response().setStatusCode(404).setStatusMessage("WTF ?").end();
        }
    }


    private void sendResponse(HttpServerRequest req, String message){
        req.response().putHeader(HttpHeaders.Names.CONTENT_TYPE	, "application/json; charset=UTF-8").end(message);
    }

}