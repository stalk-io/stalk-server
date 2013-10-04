package io.stalk.common.oauth.strategy;

import io.stalk.common.oauth.OAuthConfig;
import io.stalk.common.oauth.exception.SocialAuthConfigurationException;
import io.stalk.common.oauth.exception.SocialAuthException;
import io.stalk.common.oauth.utils.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

public class Hybrid implements OAuthStrategyBase {

    private Map<String, String> endpoints;
    private String scope;
    private String providerId;
    private OAuthConsumer oauth;
    private String successUrl;

    public Hybrid(final OAuthConfig config, final Map<String, String> endpoints) {
        this.oauth = new OAuthConsumer(config);
        this.endpoints = endpoints;
        this.providerId = config.getId();
        this.successUrl = config.get_callbackUrl();
    }

    @Override
    public RequestToken getLoginRedirectURL() throws Exception {

        String associationURL = OpenIdConsumer.getAssociationURL(endpoints
                .get(Constants.OAUTH_REQUEST_TOKEN_URL));
        Response r = HttpUtil.doHttpRequest(associationURL,
                MethodType.GET.toString(), null, null);

        StringBuffer sb = new StringBuffer();
        String assocHandle = "";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    r.getInputStream(), "UTF-8"));
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
                if ("assoc_handle:".equals(line.substring(0, 13))) {
                    assocHandle = line.substring(13);
                    break;
                }
            }
            System.out.println("ASSOCCIATION : " + assocHandle);
        } catch (Exception exc) {
            throw new SocialAuthException("Failed to read response from  ");
        }

        String realm;
        if (successUrl.indexOf("/", 9) > 0) {
            realm = successUrl.substring(0, successUrl.indexOf("/", 9));
        } else {
            realm = successUrl;
        }

        String consumerURL = realm.replace("http://", "");
        consumerURL = consumerURL.replace("https://", "");
        consumerURL = consumerURL.replaceAll(":{1}\\d*", "");

        String url = OpenIdConsumer.getRequestTokenURL(
                endpoints.get(Constants.OAUTH_REQUEST_TOKEN_URL), successUrl,
                realm, assocHandle, consumerURL, scope);
        System.out.println("Redirection to following URL should happen : " + url);


        AccessGrant requestToken = new AccessGrant();
        requestToken.setProviderId(providerId);
        return new RequestToken(url, requestToken);
    }

    @Override
    public AccessGrant verifyResponse(AccessGrant requestToken,
                                      Map<String, String> requestParams) throws Exception {
        return verifyResponse(requestToken, requestParams, MethodType.GET.toString());
    }

    @Override
    public AccessGrant verifyResponse(AccessGrant token,
                                      Map<String, String> requestParams, String methodType)
            throws Exception {

        String reqTokenStr = "";
        AccessGrant accessToken = null;

        if (this.scope != null) {
            //if (Permission.AUTHENTICATE_ONLY.equals(this.permission)) {
            //	accessToken = new AccessGrant();
            //} else {
            if (requestParams.get(OpenIdConsumer.OPENID_REQUEST_TOKEN) != null) {
                reqTokenStr = HttpUtil.decodeURIComponent(requestParams
                        .get(OpenIdConsumer.OPENID_REQUEST_TOKEN));
            }
            AccessGrant requestToken = new AccessGrant();
            requestToken.setKey(reqTokenStr);

            accessToken = oauth.getAccessToken(
                    endpoints.get(Constants.OAUTH_ACCESS_TOKEN_URL),
                    requestToken);
            if (accessToken == null) {
                throw new SocialAuthConfigurationException(
                        "Application keys may not be correct. "
                                + "The server running the application should be same that was registered to get the keys.");
            }
            //}
            for (Map.Entry<String, String> entry : requestParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                accessToken.setAttribute(key, value);
            }

            accessToken.setProviderId(providerId);
        } else {
            System.out.println("No Scope is given for the  Provider : " + providerId);
        }
        return accessToken;
    }

    @Override
    public Response executeFeed(AccessGrant accessToken, String url)
            throws Exception {
        if (accessToken == null) {
            throw new SocialAuthException(
                    "Please call verifyResponse function first to get Access Token");
        }
        return oauth.httpGet(url, null, accessToken);
    }

    @Override
    public Response executeFeed(AccessGrant accessToken, String url,
                                String methodType, Map<String, String> params,
                                Map<String, String> headerParams, String body) throws Exception {
        Response response = null;
        if (accessToken == null) {
            throw new SocialAuthException(
                    "Please call verifyResponse function first to get Access Token");
        }
        if (MethodType.GET.toString().equals(methodType)) {
            try {
                response = oauth.httpGet(url, headerParams, accessToken);
            } catch (Exception ie) {
                throw new SocialAuthException(
                        "Error while making request to URL : " + url, ie);
            }
        } else if (MethodType.PUT.toString().equals(methodType)) {
            try {
                response = oauth.httpPut(url, params, headerParams, body,
                        accessToken);
            } catch (Exception e) {
                throw new SocialAuthException(
                        "Error while making request to URL : " + url, e);
            }
        }
        return response;
    }


    @Override
    public void setScope(final String scope) {
        this.scope = scope;

    }

    @Override
    public void setAccessTokenParameterName(
            final String accessTokenParameterName) {
        // It is not implemented for Hybrid

    }

}
