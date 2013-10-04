package io.stalk.server.verticle;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public abstract class AbstractVerticle extends Verticle implements Handler<Message<JsonObject>> {

    private Logger logger;
    private String address;

    @Override
    public void start() {

        address = this.getClass().getName();

        logger = container.logger();

        start(container.config());

        vertx.eventBus().registerHandler(address, this, new AsyncResultHandler<Void>() {
            public void handle(AsyncResult<Void> asyncResult) {
                INFO(" > [" + address+ "] is registered. - " + asyncResult);
            }
        });

    }

    abstract void start(JsonObject appConfig);

    protected void DEBUG(String message, Object... args ){
        if(logger != null) logger.info( " DEBUG["+address+"] "+String.format(message, args));
    }
    protected void INFO(String message, Object... args ){
        if(logger != null) logger.info( "  INFO["+address+"] "+String.format(message, args));
    }
    protected void ERROR(String message, Object... args ){
        if(logger != null) logger.error(" ERROR["+address+"] "+String.format(message, args));
    }


    protected void sendError(Message<JsonObject> message, String error) {
        sendError(message, error, null);
    }

    protected void sendError(Message<JsonObject> message, String error, Exception e) {
        logger.error(error, e);
        JsonObject json = new JsonObject().putString("status", "error").putString("message", error);
        message.reply(json);
    }

    protected void sendStatus(String status, Message<JsonObject> message, JsonObject json) {
        if (json == null) {
            json = new JsonObject();
        }
        json.putString("status", status);
        message.reply(json);
    }

    protected void sendOK(Message<JsonObject> message) {
        sendOK(message, null);
    }
    protected void sendOK(Message<JsonObject> message, JsonObject json) {
        sendStatus("ok", message, json);
    }

}
