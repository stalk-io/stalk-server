package io.stalk.server.verticle;

import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public class SampleVerticle extends AbstractVerticle{
    @Override
    void start(JsonObject appConfig) {
        System.out.println("ABCDE --------------------------------") ;
    }

    @Override
    public void handle(Message<JsonObject> jsonObjectMessage) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
