package io.stalk.common.oauth.provider;

import io.stalk.common.oauth.OAuthConfig;
import io.stalk.common.oauth.Profile;
import io.stalk.common.oauth.exception.UserDeniedPermissionException;
import io.stalk.common.oauth.strategy.Hybrid;
import io.stalk.common.oauth.strategy.OAuthStrategyBase;
import io.stalk.common.oauth.strategy.RequestToken;
import io.stalk.common.oauth.utils.AccessGrant;
import io.stalk.common.oauth.utils.Constants;
import io.stalk.common.oauth.utils.OpenIdConsumer;

import java.util.HashMap;
import java.util.Map;

public class GoogleImpl extends AbstractProvider implements AuthProvider {

    private static final String OAUTH_SCOPE = "http://www.google.com/m8/feeds/";
    private static final Map<String, String> ENDPOINTS;

    private OAuthConfig config;
    private OAuthStrategyBase authenticationStrategy;

    static {
        ENDPOINTS = new HashMap<String, String>();
        ENDPOINTS.put(Constants.OAUTH_REQUEST_TOKEN_URL,
                "https://www.google.com/accounts/o8/ud");
        ENDPOINTS.put(Constants.OAUTH_ACCESS_TOKEN_URL,
                "https://www.google.com/accounts/OAuthGetAccessToken");
    }

    public GoogleImpl(final OAuthConfig providerConfig) throws Exception {
        config = providerConfig;
        authenticationStrategy = new Hybrid(config, ENDPOINTS);
        authenticationStrategy.setScope(getScope());
    }

    @Override
    public RequestToken getLoginRedirectURL() throws Exception {
        return authenticationStrategy.getLoginRedirectURL();
    }

    @Override
    public Profile connect(AccessGrant requestToken, final Map<String, String> requestParams)
            throws Exception {
        if (requestParams.get("openid.mode") != null
                && "cancel".equals(requestParams.get("openid.mode"))) {
            throw new UserDeniedPermissionException();
        }
        AccessGrant accessToken = authenticationStrategy.verifyResponse(requestToken, requestParams);
        return getProfile(accessToken, requestParams);
    }

    private Profile getProfile(final AccessGrant accessToken, final Map<String, String> requestParams) {
        Profile userProfile = OpenIdConsumer.getUserInfo(requestParams);
        //userProfile.setProviderId(getProviderId());
        System.out.println("User Info : " + userProfile.toString());
        return userProfile;
    }

    @Override
    public String getProviderId() {
        return config.getId();
    }


    private String getScope() {
        return OAUTH_SCOPE;
    }
}
