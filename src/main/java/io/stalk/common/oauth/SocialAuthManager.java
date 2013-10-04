package io.stalk.common.oauth;

import io.stalk.common.oauth.exception.SocialAuthConfigurationException;
import io.stalk.common.oauth.provider.AuthProvider;
import io.stalk.common.oauth.strategy.RequestToken;
import io.stalk.common.oauth.utils.AccessGrant;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class SocialAuthManager {

    private Map<String, AuthProvider> providersMap;

    private SocialAuthConfig socialAuthConfig;

    public SocialAuthManager() {
        providersMap = new HashMap<String, AuthProvider>();
    }

    public void setSocialAuthConfig(final SocialAuthConfig socialAuthConfig)
            throws Exception {
        if (socialAuthConfig == null) {
            throw new SocialAuthConfigurationException("SocialAuthConfig is null");
        }
        this.socialAuthConfig = socialAuthConfig;
    }

    public RequestToken getAuthenticationUrl(final String id)
            throws Exception {

        if (providersMap.get(id) == null) {
            providersMap.put(id, getProviderInstance(id));
        }

        return providersMap.get(id).getLoginRedirectURL();
    }

    public Profile connect(final String id, final Map<String, String> requestParams, AccessGrant requestToken)
            throws Exception {

        if (providersMap.get(id) == null) {
            providersMap.put(id, getProviderInstance(id));
        }

        return providersMap.get(id).connect(requestToken, requestParams);

    }

    private AuthProvider getProviderInstance(final String id) throws Exception {
        OAuthConfig config = socialAuthConfig.getProviderConfig(id);
        Class<?> obj = config.getProviderImplClass();
        AuthProvider provider;
        try {
            Constructor<?> cons = obj.getConstructor(OAuthConfig.class);
            provider = (AuthProvider) cons.newInstance(config);
        } catch (NoSuchMethodException me) {
            provider = (AuthProvider) obj.newInstance();
        } catch (Exception e) {
            throw new SocialAuthConfigurationException(e);
        }
        return provider;
    }


}
