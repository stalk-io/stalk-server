package io.stalk.server;

import io.netty.handler.codec.http.*;
import io.stalk.common.oauth.Profile;
import io.stalk.common.oauth.SocialAuthConfig;
import io.stalk.common.oauth.SocialAuthManager;
import io.stalk.common.oauth.strategy.RequestToken;
import io.stalk.common.oauth.utils.AccessGrant;
import io.stalk.server.verticle.NodeWatchManager;
import io.stalk.server.verticle.PublishManager;
import io.stalk.server.verticle.SessionManager;
import org.apache.commons.lang.StringUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import java.util.*;

public class WebServer extends Server{

    private EventBus eb;

    private SocialAuthManager authManager;

    interface OAUTH_COOKIE {
        String NAME 	= "STALK";

        interface VALUE{
            String CHANNEL 			= "_channel";
            String SOCKET_ID 		= "_socketId";
            String REFER 			= "_refer";
            String TARGET 			= "_target";
            String REQUEST_TOKEN 	= "_requestToken";
        }
    }

    @Override
    public void start(JsonObject appConfig, HttpServer server) {

        eb = vertx.eventBus();

        List<Module> deployments = new ArrayList<Module>();
        deployments.add(new Module(SessionManager.class.getName()   , appConfig             ));
        deployments.add(new Module(PublishManager.class.getName()                           ));
        deployments.add(new Module(NodeWatchManager.class.getName() , appConfig, 1, false   ));

        deployWorkerVerticles(deployments, new Handler<Void>() {
            public void handle(Void event) {
                INFO(" *** modules are deployed. *** ");
                eb.send(NodeWatchManager.class.getName(), new JsonObject().putString("action", "watch"));
            }
        });

        try {

            JsonObject oauthConf = appConfig.getObject("oauth", null);
            SocialAuthConfig socialAuthConfig = SocialAuthConfig.getDefault();
            socialAuthConfig.load( oauthConf );

            authManager = new SocialAuthManager();
            authManager.setSocialAuthConfig(socialAuthConfig);

        } catch (Exception e) {
            e.printStackTrace();
            // @ TODO 에러나면 실행안되도록 해야 함!!!
        }
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public void handle(final HttpServerRequest req) {

        if("/node".equals(req.path()) &&!StringUtils.isEmpty(req.params().get("refer"))){
            JsonObject reqJson = new JsonObject();
            reqJson.putString("action"	, "in");
            reqJson.putString("refer"	, req.params().get("refer"));

            eb.send(SessionManager.class.getName(), reqJson, new Handler<Message<JsonObject>>() {
                public void handle(Message<JsonObject> message) {

                    StringBuffer returnStr = new StringBuffer("");
                    if(StringUtils.isEmpty(req.params().get("callback"))){
                        returnStr.append(message.body().encode());
                    }else{
                        returnStr
                                .append(req.params().get("callback"))
                                .append("(")
                                .append(message.body().encode())
                                .append(");");
                    }

                    sendJSONResponse(req, returnStr.toString());

                }
            });

        }else if("/auth".equals(req.path())){

            String target = req.params().get("target");

            try {

                RequestToken requestToken = authManager.getAuthenticationUrl(target);

                JsonObject cookieJsonObject = new JsonObject();
                cookieJsonObject.putString(OAUTH_COOKIE.VALUE.CHANNEL	, req.params().get("channel")	);
                cookieJsonObject.putString(OAUTH_COOKIE.VALUE.SOCKET_ID	, req.params().get("socketId")	);
                cookieJsonObject.putString(OAUTH_COOKIE.VALUE.REFER		, req.params().get("refer")		);
                cookieJsonObject.putString(OAUTH_COOKIE.VALUE.TARGET	, req.params().get("target")	);

                if(requestToken.getAccessGrant() != null) {
                    cookieJsonObject.putString(OAUTH_COOKIE.VALUE.REQUEST_TOKEN, getJsonObjectFromAccessGrant(requestToken.getAccessGrant()).encode());
                }

                // Set Cookies
                createCookie(req, cookieJsonObject);
                sendHTMLResponse(req, "Loading.......<br><br><br><script type='text/javascript'>location.href='" + requestToken.getUrl() + "';</script>");

            } catch (Exception e) {
                e.printStackTrace();

                deleteCookie(req);
                sendHTMLResponse(req, "<html><body><h1>^^</h1><br>" + e.getMessage() + "</body></html>");
            }

        }else if("/auth/callback".equals(req.path())){

            String value = req.headers().get(HttpHeaders.Names.COOKIE);
            DEBUG("cookie string : %s ", value);
            Set<Cookie> cookies = CookieDecoder.decode(value);

            String refer 		= "";
            String channel 		= "";
            String socketId 	= "";
            String target	 	= "";

            AccessGrant accessToken = null;
            for (Cookie cookie : cookies) {
                DEBUG("cookies : %s", cookie.toString());
                if (cookie.getName().equals(OAUTH_COOKIE.NAME)) {
                    JsonObject cookieJson = new JsonObject(cookie.getValue());

                    DEBUG("STALK cookies : %s", cookieJson.encode());

                    JsonObject json = new JsonObject(cookieJson.getString(OAUTH_COOKIE.VALUE.REQUEST_TOKEN));

                    if(json != null){
                        accessToken = new AccessGrant();
                        accessToken.setKey(json.getString("key"));
                        accessToken.setSecret(json.getString("secret"));
                        accessToken.setProviderId(json.getString("providerId"));
                        JsonObject attrJson = json.getObject("attributes");

                        if(attrJson != null){
                            Map<String, Object> attributes = attrJson.toMap();
                            accessToken.setAttributes(attributes);
                        }
                    }

                    channel 	= cookieJson.getString(OAUTH_COOKIE.VALUE.CHANNEL	);
                    socketId 	= cookieJson.getString(OAUTH_COOKIE.VALUE.SOCKET_ID	);
                    refer 		= cookieJson.getString(OAUTH_COOKIE.VALUE.REFER		);
                    target 		= cookieJson.getString(OAUTH_COOKIE.VALUE.TARGET	);

                    break;
                }
            }

            if(target != null){
                try {

                    Map<String, String> requestParams = new HashMap<String, String>();
                    for (Map.Entry<String, String> entry : req.params()) {
                        requestParams.put(entry.getKey(), entry.getValue());
                    }

                    Profile user = authManager.connect(target, requestParams, accessToken);

                    DEBUG("OAUTH Profile : %s ", user.toString());

                    JsonObject profileJson = new JsonObject();
                    profileJson.putString("name"	, user.getName());
                    profileJson.putString("link"	, user.getLink());
                    profileJson.putString("target"	, target);

                    JsonObject jsonMessage = new JsonObject();
                    jsonMessage.putString("action"		, "pub");
                    jsonMessage.putString("type"		, "LOGIN");
                    jsonMessage.putString("channel"		, channel);
                    jsonMessage.putString("socketId"	, socketId);
                    jsonMessage.putString("refer"		, refer);
                    jsonMessage.putObject("user"		, profileJson);

                    eb.send(PublishManager.class.getName(), jsonMessage);

                    deleteCookie(req);
                    sendHTMLResponse(req, "<script type='text/javascript'>window.close();</script>");

                } catch (Exception e) {
                    e.printStackTrace();
                    sendHTMLResponse(req, "<script type='text/javascript'>window.close();</script>");
                    //sendHTMLResponse(req, "<html><body><h1>^^</h1><br>" + e.getMessage() + "</body></html>");
                }
            }else{

                deleteCookie(req);
                sendHTMLResponse(req, "<script type='text/javascript'>alert('... denied. plz check again.');window.close();</script>");
            }

        }else{
            req.response().setStatusCode(404).setStatusMessage("What'sUP!! :<").end();
        }
    }


    private JsonObject getJsonObjectFromAccessGrant(AccessGrant accessGrant) {

        JsonObject json = new JsonObject();
        json.putString("key", accessGrant.getKey());
        json.putString("secret", accessGrant.getSecret());
        json.putString("providerId", accessGrant.getProviderId());

        Map<String, Object> attributes = accessGrant.getAttributes();
        if(attributes != null){
            JsonObject jsonAttr = new JsonObject();
            for (String key : attributes.keySet()) {
                jsonAttr.putString(key, attributes.get(key).toString());
            }
            json.putObject("attributes", jsonAttr);
        }
        return json;
    }

    private void createCookie(HttpServerRequest req, JsonObject obj){


        req.response().headers().set(HttpHeaders.Names.SET_COOKIE , ServerCookieEncoder.encode(OAUTH_COOKIE.NAME, obj.encode()));

        // @ TODO 삭제 !! (netty 3 -> netty 4)
        //CookieEncoder httpCookieEncoder = new CookieEncoder(true);
        //httpCookieEncoder.addCookie(OAUTH_COOKIE.NAME		    , obj.encode());
        //req.response().headers().set(HttpHeaders.Names.SET_COOKIE   , httpCookieEncoder.encode());

    }

    private void deleteCookie(HttpServerRequest req){

        Cookie cookie = new DefaultCookie(OAUTH_COOKIE.NAME, "");
        cookie.setMaxAge(0);
        req.response().headers().set(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.encode(cookie));

        // @ TODO 삭제 !! (netty 3 -> netty 4)
        //CookieEncoder httpCookieEncoder = new CookieEncoder(true);
        //Cookie cookie = new DefaultCookie(OAUTH_COOKIE.NAME, "");
        //cookie.setMaxAge(0);
        //httpCookieEncoder.addCookie(cookie);
        //req.response().headers().set(HttpHeaders.Names.SET_COOKIE, httpCookieEncoder.encode());
    }

    private void sendJSONResponse(HttpServerRequest req, String message){
        req.response().putHeader(HttpHeaders.Names.CONTENT_TYPE	, "application/json; charset=UTF-8").end(message);
    }

    private void sendHTMLResponse(HttpServerRequest req, String message){
        req.response().putHeader(HttpHeaders.Names.CONTENT_TYPE	, "text/html; charset=UTF-8").end(message);
    }

}