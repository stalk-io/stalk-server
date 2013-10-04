package io.stalk.common.oauth.provider;

import io.stalk.common.oauth.OAuthConfig;
import io.stalk.common.oauth.Profile;
import io.stalk.common.oauth.exception.ServerDataException;
import io.stalk.common.oauth.exception.SocialAuthException;
import io.stalk.common.oauth.exception.UserDeniedPermissionException;
import io.stalk.common.oauth.strategy.OAuth2;
import io.stalk.common.oauth.strategy.OAuthStrategyBase;
import io.stalk.common.oauth.strategy.RequestToken;
import io.stalk.common.oauth.utils.AccessGrant;
import io.stalk.common.oauth.utils.Constants;
import io.stalk.common.oauth.utils.MethodType;
import io.stalk.common.oauth.utils.Response;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class GooglePlusImpl extends AbstractProvider implements AuthProvider {

    private static final String PROFILE_URL = "https://www.googleapis.com/oauth2/v1/userinfo";
    private static final String SCOPE = "https://www.googleapis.com/auth/userinfo.profile";
    private static final Map<String, String> ENDPOINTS;

    private OAuthConfig config;
    private OAuthStrategyBase authenticationStrategy;

    static {
        ENDPOINTS = new HashMap<String, String>();
        ENDPOINTS.put(
                Constants.OAUTH_AUTHORIZATION_URL,
                "https://accounts.google.com/o/oauth2/auth");
        ENDPOINTS.put(
                Constants.OAUTH_ACCESS_TOKEN_URL,
                "https://accounts.google.com/o/oauth2/token");
    }

    public GooglePlusImpl(final OAuthConfig providerConfig) throws Exception {
        config = providerConfig;
        authenticationStrategy = new OAuth2(config, ENDPOINTS);
        authenticationStrategy.setScope(SCOPE);
    }

    @Override
    public RequestToken getLoginRedirectURL() throws Exception {
        return authenticationStrategy.getLoginRedirectURL();
    }

    @Override
    public Profile connect(AccessGrant requestToken, Map<String, String> requestParams) throws Exception {
        if (requestParams.get("error_reason") != null
                && "user_denied".equals(requestParams.get("error_reason"))) {
            throw new UserDeniedPermissionException();
        }
        AccessGrant accessGrant = authenticationStrategy.verifyResponse(null, requestParams, MethodType.POST.toString());

        if (accessGrant != null) {

            return authFacebookLogin(accessGrant);
        } else {
            throw new SocialAuthException("Access token not found");
        }
    }

    private Profile authFacebookLogin(AccessGrant accessGrant) throws Exception {
        String presp;

        try {
            Response response = authenticationStrategy.executeFeed(accessGrant, PROFILE_URL);
            presp = response.getResponseBodyAsString(Constants.ENCODING);
        } catch (Exception e) {
            throw new SocialAuthException("Error while getting profile from "
                    + PROFILE_URL, e);
        }
        try {
            System.out.println("User Profile : " + presp);
            JSONObject resp = new JSONObject(presp);
            Profile p = new Profile();

            if (resp.has("link")) {
                p.setLink(resp.getString("link"));
            }
            if (resp.has("name")) {
                p.setName(resp.getString("name"));
            } else {
                p.setName(resp.getString("first_name") + " " + resp.getString("last_name"));
            }

            return p;

        } catch (Exception ex) {
            throw new ServerDataException(
                    "Failed to parse the user profile json : " + presp, ex);
        }
    }

    @Override
    public String getProviderId() {
        return config.getId();
    }

}