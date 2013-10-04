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

public class TwitterImpl extends AbstractProvider implements AuthProvider {


    // https://dev.twitter.com/docs/api/1.1/get/users/show (2013 추석에 청주에서!!)
    private static final String PROFILE_URL = "http://api.twitter.com/1.1/users/show.json?screen_name=";
    private static final Map<String, String> ENDPOINTS;

    private OAuthConfig config;
    private OAuthStrategyBase authenticationStrategy;

    static {
        ENDPOINTS = new HashMap<String, String>();
        ENDPOINTS.put(Constants.OAUTH_REQUEST_TOKEN_URL,
                "http://api.twitter.com/oauth/request_token");
        ENDPOINTS.put(Constants.OAUTH_AUTHORIZATION_URL,
                "https://api.twitter.com/oauth/authenticate");
        ENDPOINTS.put(Constants.OAUTH_ACCESS_TOKEN_URL,
                "https://api.twitter.com/oauth/access_token");
    }

    public TwitterImpl(final OAuthConfig providerConfig) throws Exception {
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
        String url = PROFILE_URL + accessToken.getAttribute("screen_name");

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
            /*
                    JSONObject pObj = new JSONObject(result);
			if (pObj.has("id_str")) {
				profile.setValidatedId(pObj.getString("id_str"));
			}
			if (pObj.has("name")) {
				profile.setFullName(pObj.getString("name"));
			}
			if (pObj.has("location")) {
				profile.setLocation(pObj.getString("location"));
			}
			if (pObj.has("screen_name")) {
				profile.setDisplayName(pObj.getString("screen_name"));
			}
			if (pObj.has("lang")) {
				profile.setLanguage(pObj.getString("lang"));
			}
			if (pObj.has("profile_image_url")) {
				profile.setProfileImageURL(pObj.getString("profile_image_url"));
			}
			profile.setProviderId(getProviderId());
*/

            JSONObject pObj = new JSONObject(result);
            if (pObj.has("name")) {
                profile.setName(pObj.getString("name"));
            }
            if (pObj.has("screen_name")) {
                profile.setLink("https://twitter.com/" + pObj.getString("screen_name"));
            } else {
                profile.setLink(pObj.getString("link"));
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