package io.stalk.server;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class Server extends Verticle implements Handler<HttpServerRequest> {

    private Logger logger;
    private String address;

    @Override
    public void start() {

        address = this.getClass().getName();

        logger = container.logger();
        JsonObject appConfig = container.config();
        HttpServer server = vertx.createHttpServer();
        server.requestHandler(this);

        start(appConfig, server);

        int serverPort = appConfig.getInteger("port", 8080).intValue();

        server.listen(serverPort);

        postStart(appConfig, server);

    }

    abstract void start(JsonObject appConfig, HttpServer server);
    protected void postStart(JsonObject appConfig, HttpServer server){
        INFO("server is listening : "+ appConfig.getInteger("port", 8080) + " / "+server.hashCode());
    }

    protected void DEBUG(String message, Object... args ){
        if(logger != null) logger.info(" DEBUG["+address+"] "+String.format(message, args));
    }
    protected void INFO(String message, Object... args ){
        if(logger != null) logger.info("  INFO["+address+"] "+String.format(message, args));
    }
    protected void ERROR(String message, Object... args ){
        if(logger != null) logger.info(" ERROR["+address+"] "+String.format(message, args));
    }



    public void deployWorkerVerticles(final Module[] modules, final Handler<Void> doneHandler) {

        List<Module> deployments = new ArrayList<Module>();
        for(Module module : modules){
            deployments.add(
                    new Module(module.getModuleName(), module.getModuleConfig(), module.getInstances(), module.isMultiThreaded())
            );
        };
        deployWorkerVerticles(deployments, doneHandler);
    }

    public void deployWorkerVerticles(final List<Module> modules, final Handler<Void> doneHandler) {

        final Iterator<Module> it = modules.iterator();

        final AsyncResultHandler<String> handler = new AsyncResultHandler<String>() {
            public void handle(AsyncResult<String> asyncResult) {

                if(it.hasNext()) {
                    Module deployment = it.next();

                    INFO("Deploying module: " + deployment.getModuleName());

                    container.deployWorkerVerticle(
                            deployment.getModuleName(),
                            deployment.getModuleConfig(),
                            deployment.getInstances(),
                            deployment.isMultiThreaded(),
                            this);
                } else {
                    doneHandler.handle(null);
                }
            }
        };

        handler.handle(null);
    }

}
