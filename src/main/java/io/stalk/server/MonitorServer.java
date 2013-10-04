package io.stalk.server;


import io.stalk.server.verticle.SampleVerticle;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Simple sample Verticle Application for dev.
 */
public class MonitorServer extends Server{

    private HttpClient client;
    private EventBus eb;
    private Logger logger;


    @Override
    void start(JsonObject appConfig, HttpServer server) {

        container.deployWorkerVerticle(SampleVerticle.class.getName(),new JsonObject(),1,false);


        eb = vertx.eventBus();
        logger = container.logger();

        client = vertx.createHttpClient().setPort(8080).setHost("localhost");
        client.setConnectTimeout(5000);
        System.out.println("SERVER --------");

        /*
        long timerID = vertx.setPeriodic(1000, new Handler<Long>() {
            public void handle(Long timerID) {
                logger.info("And every second this is printed");
            }
        });
        System.out.print(timerID);
        */
    }

    @Override
    public void handle(HttpServerRequest httpServerRequest) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
