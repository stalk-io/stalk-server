package io.stalk.common.oauth.provider;

import io.stalk.common.oauth.OAuthConfig;
import io.stalk.common.oauth.Profile;
import io.stalk.common.oauth.exception.ServerDataException;
import io.stalk.common.oauth.exception.SocialAuthException;
import io.stalk.common.oauth.exception.UserDeniedPermissionException;
import io.stalk.common.oauth.strategy.OAuth1;
import io.stalk.common.oauth.strategy.OAuthStrategyBase;
import io.stalk.common.oauth.strategy.RequestToken;
import io.stalk.common.oauth.utils.AccessGrant;
import io.stalk.common.oauth.utils.Constants;
import io.stalk.common.oauth.utils.Response;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class YahooImpl extends AbstractProvider implements AuthProvider {

    private static final String PROFILE_URL = "http://social.yahooapis.com/v1/user/%1$s/profile?format=json";
    private static final Map<String, String> ENDPOINTS;

    private OAuthConfig config;
    private OAuthStrategyBase authenticationStrategy;

    static {
        ENDPOINTS = new HashMap<String, String>();
        ENDPOINTS.put(Constants.OAUTH_REQUEST_TOKEN_URL,
                "https://api.login.yahoo.com/oauth/v2/get_request_token");
        ENDPOINTS.put(Constants.OAUTH_AUTHORIZATION_URL,

                "https://api.login.yahoo.com/oauth/v2/request_auth");
        ENDPOINTS.put(Constants.OAUTH_ACCESS_TOKEN_URL,
                "https://api.login.yahoo.com/oauth/v2/get_token");
    }

    public YahooImpl(final OAuthConfig providerConfig) throws Exception {
        config = providerConfig;
        authenticationStrategy = new OAuth1(config, ENDPOINTS);
    }

    @Override
    public RequestToken getLoginRedirectURL() throws Exception {
        return authenticationStrategy.getLoginRedirectURL();
    }


    @Override
    public Profile connect(final AccessGrant requestToken, final Map<String, String> requestParams)
            throws Exception {

        if (requestParams.get("denied") != null) {
            throw new UserDeniedPermissionException();
        }
        AccessGrant accessToken = authenticationStrategy.verifyResponse(requestToken, requestParams);
        return getProfile(accessToken);
    }

    private Profile getProfile(AccessGrant accessToken) throws Exception {

        Profile profile = new Profile();
        String guid = (String) accessToken.getAttribute("xoauth_yahoo_guid");
        if (guid.indexOf("<") != -1) {
            guid = guid.substring(0, guid.indexOf("<")).trim();
            accessToken.setAttribute("xoauth_yahoo_guid", guid);
        }

        String url = String.format(PROFILE_URL, guid);
        Response serviceResponse = null;

        try {
            serviceResponse = authenticationStrategy.executeFeed(accessToken, url);
        } catch (Exception e) {
            throw new SocialAuthException(
                    "Failed to retrieve the user profile from  " + url, e);
        }
        if (serviceResponse.getStatus() != 200) {
            throw new SocialAuthException(
                    "Failed to retrieve the user profile from  " + url
                            + ". Staus :" + serviceResponse.getStatus());
        }
        String result;
        try {
            result = serviceResponse
                    .getResponseBodyAsString(Constants.ENCODING);
            System.out.println("User Profile :" + result);
        } catch (Exception exc) {
            throw new SocialAuthException("Failed to read response from  "
                    + url, exc);
        }
        try {
            JSONObject jobj = new JSONObject(result);
            if (jobj.has("profile")) {

                JSONObject pObj = jobj.getJSONObject("profile");

                System.out.println(pObj);


                if (pObj.has("nickname")) {
                    profile.setName(pObj.getString("nickname"));
                }
                if (pObj.has("screen_name")) {
                    profile.setLink("https://twitter.com/" + pObj.getString("screen_name"));
                } else {
                    profile.setLink(pObj.getString("link"));
                }


            }

            return profile;
        } catch (Exception e) {
            throw new ServerDataException(
                    "Failed to parse the user profile json : " + result, e);
        }
    }

    @Override
    public String getProviderId() {
        return config.getId();
    }

}