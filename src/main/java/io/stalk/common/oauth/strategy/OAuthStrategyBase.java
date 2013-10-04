package io.stalk.common.oauth.strategy;

import io.stalk.common.oauth.utils.AccessGrant;
import io.stalk.common.oauth.utils.Response;

import java.util.Map;

public interface OAuthStrategyBase {

    public RequestToken getLoginRedirectURL() throws Exception;

    public AccessGrant verifyResponse(
            AccessGrant requestToken,
            Map<String, String> requestParams)
            throws Exception;

    public AccessGrant verifyResponse(
            AccessGrant requestToken,
            Map<String, String> requestParams,
            String methodType) throws Exception;

    public Response executeFeed(
            AccessGrant accessGrant,
            String url) throws Exception;

    public Response executeFeed(
            AccessGrant accessGrant,
            String url,
            String methodType,
            Map<String, String> params,
            Map<String, String> headerParams,
            String body) throws Exception;

    public void setScope(final String scope);

    public void setAccessTokenParameterName(String accessTokenParameterName);


}
