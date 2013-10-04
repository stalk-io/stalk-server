package io.stalk.common.oauth;

import io.stalk.common.oauth.exception.SocialAuthConfigurationException;
import io.stalk.common.oauth.utils.Constants;
import io.stalk.common.oauth.utils.HttpUtil;
import org.vertx.java.core.json.JsonObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class SocialAuthConfig {

    private static final String OAUTH_CONSUMER_PROPS = "oauth_consumer.properties";
    private static SocialAuthConfig DEFAULT = new SocialAuthConfig();
    private Map<String, Class<?>> providersImplMap;
    private Map<String, String> domainMap;
    private Properties applicationProperties;

    private Map<String, OAuthConfig> providersConfig;

    public static SocialAuthConfig getDefault() {
        return DEFAULT;
    }

    public SocialAuthConfig() {
        providersImplMap = new HashMap<String, Class<?>>();
        providersImplMap.put(Constants.FACEBOOK, io.stalk.common.oauth.provider.FacebookImpl.class);
        //		providersImplMap.put(Constants.FOURSQUARE, io.sodabox.web.auth.provider.FourSquareImpl.class);
        providersImplMap.put(Constants.GOOGLE, io.stalk.common.oauth.provider.GoogleImpl.class);
        providersImplMap.put(Constants.GOOGLEPLUS, io.stalk.common.oauth.provider.GooglePlusImpl.class);
        //		providersImplMap.put(Constants.HOTMAIL, io.sodabox.web.auth.provider.HotmailImpl.class);
        //		providersImplMap.put(Constants.LINKEDIN, io.sodabox.web.auth.provider.LinkedInImpl.class);
        //		providersImplMap.put(Constants.MYSPACE, io.sodabox.web.auth.provider.MySpaceImpl.class);
        providersImplMap.put(Constants.TWITTER, io.stalk.common.oauth.provider.TwitterImpl.class);
        providersImplMap.put(Constants.YAHOO, io.stalk.common.oauth.provider.YahooImpl.class);
        //		providersImplMap.put(Constants.SALESFORCE, io.sodabox.web.auth.provider.SalesForceImpl.class);
        //		providersImplMap.put(Constants.YAMMER, io.sodabox.web.auth.provider.YammerImpl.class);
        //		providersImplMap.put(Constants.MENDELEY, io.sodabox.web.auth.provider.MendeleyImpl.class);
        //		providersImplMap.put(Constants.RUNKEEPER, io.sodabox.web.auth.provider.RunkeeperImpl.class);

        domainMap = new HashMap<String, String>();
        //domainMap.put(Constants.GOOGLE, "www.google.com");
        domainMap.put(Constants.GOOGLEPLUS, "www.googleapis.com");
        //domainMap.put(Constants.YAHOO, "api.login.yahoo.com");
        domainMap.put(Constants.TWITTER, "twitter.com");
        domainMap.put(Constants.FACEBOOK, "graph.facebook.com");
        //		domainMap.put(Constants.HOTMAIL, "consent.live.com");
        //		domainMap.put(Constants.LINKEDIN, "api.linkedin.com");
        //		domainMap.put(Constants.FOURSQUARE, "foursquare.com");
        //		domainMap.put(Constants.MYSPACE, "api.myspace.com");
        //		domainMap.put(Constants.SALESFORCE, "login.salesforce.com");
        //		domainMap.put(Constants.YAMMER, "www.yammer.com");
        //		domainMap.put(Constants.MENDELEY, "api.mendeley.com");
        //		domainMap.put(Constants.RUNKEEPER, "runkeeper.com");

        providersConfig = new HashMap<String, OAuthConfig>();


    }

    public void load() throws Exception {
        load(OAUTH_CONSUMER_PROPS);
    }

    public void load(final String fileName) throws Exception {
        ClassLoader loader = SocialAuthConfig.class.getClassLoader();
        try {
            InputStream in = loader.getResourceAsStream(fileName);
            load(in);
        } catch (NullPointerException ne) {
            throw new FileNotFoundException(fileName + " file is not found in your class path");
        }
    }

    public void load(final InputStream inputStream) throws Exception {

        Properties props = new Properties();
        try {
            props.load(inputStream);
            load(props);
        } catch (IOException ie) {
            throw new IOException(
                    "Could not load configuration from input stream");
        }
    }

    public void load(final Properties properties) throws Exception {
        this.applicationProperties = properties;
        loadProvidersConfig();
        String timeout = null;
        if (applicationProperties.containsKey(Constants.HTTP_CONNECTION_TIMEOUT)) {
            timeout = applicationProperties.getProperty(Constants.HTTP_CONNECTION_TIMEOUT).trim();
        }
        if (timeout != null && !timeout.isEmpty()) {
            int time = 0;
            try {
                time = Integer.parseInt(timeout);
            } catch (NumberFormatException ne) {
                // Http connection timout is not an integer in configuration
            }
            HttpUtil.setConnectionTimeout(time);
        }
    }

    public void load(JsonObject oauthConf) throws Exception {

        String timeout = oauthConf.getString(Constants.HTTP_CONNECTION_TIMEOUT);
        if (timeout != null && !timeout.isEmpty()) {
            int time = 0;
            try {
                time = Integer.parseInt(timeout);
            } catch (NumberFormatException ne) {
                // Http connection timout is not an integer in configuration
            }
            HttpUtil.setConnectionTimeout(time);
        }

        for (Map.Entry<String, String> entry : domainMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            JsonObject providerConf = oauthConf.getObject(value);

            String cKey = providerConf.getString("consumer_key");
            String cSecret = providerConf.getString("consumer_secret");
            String cUrl = providerConf.getString("callback_url", "");

            if (cKey != null && cSecret != null) {
                OAuthConfig conf = new OAuthConfig(cKey, cSecret, cUrl);
                conf.setId(key);
                conf.setProviderImplClass(providersImplMap.get(key));
                providersConfig.put(key, conf);
            } else {
                System.out.println("Configuration for provider " + key + " is not available");
            }

        }
    }

    private void loadProvidersConfig() {
        for (Map.Entry<String, String> entry : domainMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String cKey = applicationProperties.getProperty(value + ".consumer_key");
            String cSecret = applicationProperties.getProperty(value + ".consumer_secret");
            String cUrl = applicationProperties.getProperty(value + ".callback_url");
            if (cKey != null && cSecret != null) {

                System.out.println("Loading configuration for provider : " + key);

                if (cUrl == null) cUrl = "";
                OAuthConfig conf = new OAuthConfig(cKey, cSecret, cUrl);
                conf.setId(key);
                conf.setProviderImplClass(providersImplMap.get(key));
                providersConfig.put(key, conf);

            } else {
                System.out.println("Configuration for provider " + key + " is not available");
            }
        }
    }

    public OAuthConfig getProviderConfig(final String id) throws Exception {

        OAuthConfig config = providersConfig.get(id);

        if (config == null) {
            throw new SocialAuthConfigurationException(
                    "Configuration of " + id + " provider is not found");
        }
        if (config.get_consumerSecret().length() <= 0) {
            throw new SocialAuthConfigurationException(
                    id + " consumer_secret value is null");
        }
        if (config.get_consumerKey().length() <= 0) {
            throw new SocialAuthConfigurationException(
                    id + " consumer_key value is null");
        }
        return config;
    }


}
