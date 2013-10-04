package io.stalk.common.oauth.strategy;

import io.stalk.common.oauth.OAuthConfig;
import io.stalk.common.oauth.exception.SocialAuthException;
import io.stalk.common.oauth.utils.*;

import java.util.Map;

public class OAuth1 implements OAuthStrategyBase {

    private OAuthConsumer oauth;
    private Map<String, String> endpoints;
    private String scope;
    private String providerId;
    private String successUrl;

    public OAuth1(final OAuthConfig config, final Map<String, String> endpoints) {
        this.oauth = new OAuthConsumer(config);
        this.endpoints = endpoints;
        this.providerId = config.getId();
        this.successUrl = config.get_callbackUrl();
    }

    @Override
    public RequestToken getLoginRedirectURL() throws Exception {
        AccessGrant requestToken = oauth.getRequestToken(
                endpoints.get(Constants.OAUTH_REQUEST_TOKEN_URL), successUrl);
        String authUrl = endpoints.get(Constants.OAUTH_AUTHORIZATION_URL);
        if (scope != null) {
            authUrl += scope;
        }
        StringBuilder urlBuffer = oauth.buildAuthUrl(authUrl, requestToken, successUrl);
        System.out.println("Redirection to following URL should happen : " + urlBuffer.toString());

        return new RequestToken(urlBuffer.toString(), requestToken);
    }

    @Override
    public AccessGrant verifyResponse(AccessGrant requestToken,
                                      Map<String, String> requestParams) throws Exception {
        return verifyResponse(requestToken, requestParams, MethodType.GET.toString());
    }

    @Override
    public AccessGrant verifyResponse(
            AccessGrant requestToken,
            Map<String, String> requestParams, String methodType)
            throws Exception {
        if (requestToken == null) {
            throw new SocialAuthException("Request token is null");
        }
        String verifier = requestParams.get(Constants.OAUTH_VERIFIER);
        if (verifier != null) {
            requestToken.setAttribute(Constants.OAUTH_VERIFIER, verifier);
        }
        System.out.println("Call to fetch Access Token");
        AccessGrant accessToken = oauth.getAccessToken(
                endpoints.get(Constants.OAUTH_ACCESS_TOKEN_URL), requestToken);
        accessToken.setProviderId(providerId);
        return accessToken;
    }

    @Override
    public Response executeFeed(AccessGrant accessToken, String url)
            throws Exception {
        return oauth.httpGet(url, null, accessToken);
    }

    @Override
    public Response executeFeed(AccessGrant accessToken, String urlStr,
                                String methodType, Map<String, String> params,
                                Map<String, String> headerParams, String body) throws Exception {
        Response response = null;
        if (accessToken == null) {
            throw new SocialAuthException(
                    "Please call verifyResponse function first to get Access Token");
        }
        if (MethodType.GET.toString().equals(methodType)) {
            try {
                response = oauth.httpGet(urlStr, headerParams, accessToken);
            } catch (Exception ie) {
                throw new SocialAuthException(
                        "Error while making request to URL : " + urlStr, ie);
            }
        } else if (MethodType.PUT.toString().equals(methodType)) {
            try {
                response = oauth.httpPut(urlStr, params, headerParams, body,
                        accessToken);
            } catch (Exception e) {
                throw new SocialAuthException(
                        "Error while making request to URL : " + urlStr, e);
            }
        } else if (MethodType.POST.toString().equals(methodType)) {
            try {
                response = oauth.httpPost(urlStr, params, headerParams, body,
                        accessToken);
            } catch (Exception e) {
                throw new SocialAuthException(
                        "Error while making request to URL : " + urlStr, e);
            }
        }
        return response;
    }

    @Override
    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public void setAccessTokenParameterName(String accessTokenParameterName) {
        // 'accessTokenParameterName' is not implemented for OAuth1
    }


}
