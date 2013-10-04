package io.stalk.server.handler;

import io.stalk.server.verticle.SessionManager;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.shareddata.SharedData;
import org.vertx.java.core.sockjs.SockJSSocket;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class SocketMessageHandler implements Handler<SockJSSocket> {

    private EventBus eb;
    private String channel;

    private SharedData sd;
    private ConcurrentMap<String, String> sessionStore;

    private String REFER_STORAGE = "_REFER_STORAGE";

    public SocketMessageHandler(Vertx vertx, String channel) {
        this.eb = vertx.eventBus();
        this.channel = channel;
        this.sd = vertx.sharedData();
        this.sessionStore = vertx.sharedData().getMap(REFER_STORAGE);
    }

    @Override
    public void handle(final SockJSSocket sock) {

        sock.endHandler(new Handler<Void>(){
            public void handle(Void event) {

                String socketId = sock.writeHandlerID();

                Session session = getSessionInfo(socketId);

                removeSocketId(session.REFER, socketId);

                DEBUG(" ** OUT ** %s / %s ", session.REFER, session.USER);

                int cnt = getSocketsCount(session.REFER);
                if(cnt > 0){

                    JsonObject json = new JsonObject();
                    json.putString("action", "OUT");
                    json.putObject("user", new JsonObject(session.USER));
                    json.putNumber("count", cnt);

                    sendMessageToAll(session.REFER, json.encode());
                }

                eb.send(SessionManager.class.getName(),
                        new JsonObject()
                                .putString("action" , "update")
                                .putString("refer"  , session.REFER)
                                .putNumber("count"  , cnt)
                                .putString("channel", channel)
                );

                removeSessionInfo(socketId);

            }
        });



        sock.dataHandler(new Handler<Buffer>() {

            public void handle(Buffer data) {

                JsonObject reqJson = new JsonObject(data.toString());

                if( "JOIN".equals(reqJson.getString("action")) ){

                    DEBUG("[[JOIN]] : %s",reqJson.encode());

                    String 		refer 		= reqJson.getString("refer");   // location.host + location.pathname
                    JsonObject 	user 		= reqJson.getObject("user", new JsonObject());
                    String 		socketId 	= sock.writeHandlerID();

                    // add socketId for refer
                    addSocketId(refer, socketId);

                    // add session info
                    String userStr = user.encode();
                    addSessionInfo(socketId, refer, userStr);

                    Set<String> socks = getSocketIds(refer);

                    eb.send(SessionManager.class.getName(),
                            new JsonObject()
                                    .putString("action", "update")
                                    .putString("refer", refer)
                                    .putNumber("count", socks.size())
                                    .putString("channel", channel)
                    );

                    for(String target : socks){

                        if(target.equals(socketId)){ // ME !!

                            JsonObject json = new JsonObject();
                            json.putString("action"		, "JOIN");
                            json.putNumber("count"		, socks.size());
                            json.putString("socketId"	, socketId);
                            sendMessage(target, json.encode());

                        }else{

                            JsonObject json = new JsonObject();
                            json.putString("action"		, "IN");
                            json.putNumber("count"		, socks.size());
                            json.putString("user"		, userStr);
                            sendMessage(target, json.encode());
                        }

                    }

                }else if( "MESSAGE".equals(reqJson.getString("action")) ){

                    JsonObject json = new JsonObject();
                    json.putString("action"		, "MESSAGE");
                    json.putString("message"	, reqJson.getString("message"));
                    json.putObject("user"		, reqJson.getObject("user"));

                    sendMessageToAll(reqJson.getString("refer"), json.encode());

                }else if( "LOGOUT".equals(reqJson.getString("action")) ){
                    String socketId = sock.writeHandlerID();

                    Session session = getSessionInfo(socketId);

                    DEBUG(" ** LOGOUT.. ** %s / %s ", session.REFER, session.USER);

                    int cnt = getSocketsCount(session.REFER);

                    if(cnt > 0){

                        JsonObject json = new JsonObject();
                        json.putString("action"	, "LOGOUT");
                        json.putObject("user"	, new JsonObject(session.USER));
                        json.putNumber("count"	, cnt);

                        sendMessageToAll(session.REFER, json.encode());
                    }

                    removeSocketId(session.REFER, socketId);
                    removeSessionInfo(socketId);

                }else if( "SIGNAL".equals(reqJson.getString("action")) ){
                    sendMessageToAll(reqJson.getString("refer"), reqJson.encode());

                }else{
                    sock.write(data);

                }

            }
        });


    }

    class Session {

        public String REFER;
        public String USER;

        public Session(String data) {
            REFER = data.substring(0, data.indexOf("^"));
            USER  = data.substring(data.indexOf("^")+1);
        }
    }


    protected Session getSessionInfo(String socketId){
        String str = sessionStore.get(socketId);
        return new Session(str);
    }
    protected void removeSessionInfo(String socketId){
        sessionStore.remove(socketId);
    }
    protected void addSessionInfo(String socketId, String refer, String user){
        sessionStore.put(socketId, refer+"^"+user);
    }


    protected void addSocketId(String refer, String socketId){
        sd.getSet(refer).add(socketId);
    }
    protected Set<String> getSocketIds(String refer){
        return sd.getSet(refer);
    }
    protected void removeSocketId(String refer, String socketId){
        sd.getSet(refer).remove(socketId);
        if(sd.getSet(refer).size() == 0 )sd.removeSet(refer);
    }
    protected int getSocketsCount(String refer){
        return sd.getSet(refer).size();
    }

    protected void sendMessage(String socketId, String message){

        DEBUG("SEND MESSAGE %s -> %s", socketId, message);

        eb.send(socketId, new Buffer(message));
    }

    protected void sendMessageToAll(String refer, String message){
        Set<String> socks = getSocketIds(refer);
        for(String socketId : socks){
            sendMessage(socketId, message);
        }
    }

    protected void DEBUG(String message, Object... args ){
        System.out.println(" DEBUG["+this.getClass().getName()+"] "+String.format(message, args));
    }
    protected void INFO(String message, Object... args ){
        System.out.println("  INFO["+this.getClass().getName()+"] "+String.format(message, args));
    }



}
