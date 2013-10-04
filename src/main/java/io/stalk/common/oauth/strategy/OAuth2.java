package io.stalk.common.oauth.strategy;

import io.stalk.common.oauth.OAuthConfig;
import io.stalk.common.oauth.exception.SocialAuthException;
import io.stalk.common.oauth.utils.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class OAuth2 implements OAuthStrategyBase {

    private OAuthConsumer oauth;
    private Map<String, String> endpoints;
    private String scope;
    private String providerId;
    private String successUrl;
    private String accessTokenParameterName;

    public OAuth2(final OAuthConfig config, final Map<String, String> endpoints) {
        this.oauth = new OAuthConsumer(config);
        this.endpoints = endpoints;
        this.providerId = config.getId();
        this.accessTokenParameterName = Constants.ACCESS_TOKEN_PARAMETER_NAME;
        this.successUrl = config.get_callbackUrl();
        try {
            this.successUrl = URLEncoder.encode(this.successUrl, Constants.ENCODING);
        } catch (UnsupportedEncodingException e) {
            // do nothing.
        }
    }

    @Override
    public RequestToken getLoginRedirectURL() throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(endpoints.get(Constants.OAUTH_AUTHORIZATION_URL));
        char separator = endpoints.get(Constants.OAUTH_AUTHORIZATION_URL)
                .indexOf('?') == -1 ? '?' : '&';
        sb.append(separator);
        sb.append("client_id=").append(oauth.getConfig().get_consumerKey());
        sb.append("&response_type=code");
        sb.append("&display=popup");
        sb.append("&redirect_uri=").append(this.successUrl);
        if (scope != null) {
            sb.append("&scope=").append(scope);
        }
        String url = sb.toString();

        System.out.println("Redirection to following URL should happen : " + url);

        AccessGrant requestToken = new AccessGrant();
        requestToken.setProviderId(providerId);

        RequestToken token = new RequestToken(url, requestToken);

        return token;
    }

    @Override
    public AccessGrant verifyResponse(AccessGrant requestToken,
                                      Map<String, String> requestParams) throws Exception {
        return verifyResponse(null, requestParams, MethodType.GET.toString());
    }

    @Override
    public AccessGrant verifyResponse(AccessGrant requestToken,
                                      Map<String, String> requestParams, String methodType)
            throws Exception {

        String code = requestParams.get("code");
        if (code == null || code.length() == 0) {
            throw new SocialAuthException("Verification code is null");
        }

        String acode;
        String accessToken = null;
        try {
            acode = URLEncoder.encode(code, "UTF-8");
        } catch (Exception e) {
            acode = code;
        }
        StringBuffer sb = new StringBuffer();
        sb.append("client_id=").append(oauth.getConfig().get_consumerKey());
        sb.append("&redirect_uri=").append(this.successUrl);
        sb.append("&client_secret=").append(
                oauth.getConfig().get_consumerSecret());
        sb.append("&code=").append(acode);
        sb.append("&grant_type=authorization_code");

        String authURL = sb.toString();
        System.out.println("URL for Access Token request : " + authURL);

        Response response;
        try {

            if (MethodType.POST.toString().equals(methodType)) {
                response = HttpUtil.doHttpRequest(endpoints.get(Constants.OAUTH_ACCESS_TOKEN_URL), methodType, sb.toString(), null);
            } else {
                char separator = endpoints.get(Constants.OAUTH_ACCESS_TOKEN_URL)
                        .indexOf('?') == -1 ? '?' : '&';
                response = HttpUtil.doHttpRequest(endpoints.get(Constants.OAUTH_ACCESS_TOKEN_URL) + separator + sb.toString(), methodType, null, null);
            }

        } catch (Exception e) {
            throw new SocialAuthException("Error in url : " + authURL, e);
        }
        String result;
        try {
            result = response.getResponseBodyAsString(Constants.ENCODING);
        } catch (IOException io) {
            throw new SocialAuthException(io);
        }
        Map<String, Object> attributes = new HashMap<String, Object>();
        Integer expires = null;
        if (result.indexOf("{") < 0) {
            String[] pairs = result.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=");
                if (kv.length != 2) {
                    throw new SocialAuthException(
                            "Unexpected auth response from " + authURL);
                } else {
                    if (kv[0].equals("access_token")) {
                        accessToken = kv[1];
                    } else if (kv[0].equals("expires")) {
                        expires = Integer.valueOf(kv[1]);
                    } else {
                        attributes.put(kv[0], kv[1]);
                    }
                }
            }
        } else {
            try {
                JSONObject jObj = new JSONObject(result);
                if (jObj.has("access_token")) {
                    accessToken = jObj.getString("access_token");
                }
                if (jObj.has("expires_in")) {
                    String str = jObj.getString("expires_in");
                    if (!str.isEmpty()) {
                        expires = Integer.valueOf(str);
                    }
                }
                if (accessToken != null) {
                    Iterator<String> keyItr = jObj.keys();
                    while (keyItr.hasNext()) {
                        String key = keyItr.next();
                        if (!"access_token".equals(key)
                                && !"expires_in".equals(key)) {
                            attributes.put(key, jObj.get(key));
                        }
                    }
                }
            } catch (JSONException je) {
                throw new SocialAuthException("Unexpected auth response from "
                        + authURL);
            }
        }
        System.out.println("Access Token : " + accessToken);
        System.out.println("Expires : " + expires);

        AccessGrant accessGrant = new AccessGrant();

        if (accessToken != null) {

            accessGrant.setKey(accessToken);
            accessGrant.setAttribute(Constants.EXPIRES, expires);
            if (attributes.size() > 0) {
                accessGrant.setAttributes(attributes);
            }
            /*if (permission != null) {
                accessGrant.setPermission(permission);
			} else {
				accessGrant.setPermission(Permission.ALL);
			}*/
            accessGrant.setProviderId(providerId);
        } else {
            throw new SocialAuthException(
                    "Access token and expires not found from " + authURL);
        }
        return accessGrant;
    }

    @Override
    public Response executeFeed(AccessGrant accessGrant, String url) throws Exception {
        if (accessGrant == null) {
            throw new SocialAuthException(
                    "Please call verifyResponse function first to get Access Token");
        }
        char separator = url.indexOf('?') == -1 ? '?' : '&';
        String urlStr = url + separator + accessTokenParameterName + "=" + accessGrant.getKey();

        return HttpUtil.doHttpRequest(urlStr, MethodType.GET.toString(), null, null);
    }

    @Override
    public Response executeFeed(AccessGrant accessGrant, String url, String methodType,
                                Map<String, String> params, Map<String, String> headerParams,
                                String body) throws Exception {
        if (accessGrant == null) {
            throw new SocialAuthException(
                    "Please call verifyResponse function first to get Access Token");
        }
        String reqURL = url;
        String bodyStr = body;
        StringBuffer sb = new StringBuffer();
        sb.append(accessTokenParameterName).append("=")
                .append(accessGrant.getKey());
        if (params != null && params.size() > 0) {
            for (String key : params.keySet()) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                sb.append(key).append("=").append(params.get(key));
            }
        }
        if (MethodType.GET.toString().equals(methodType)) {
            if (sb.length() > 0) {
                int idx = url.indexOf('?');
                if (idx == -1) {
                    reqURL += "?";
                }
                reqURL += sb.toString();
            }
        } else if (MethodType.POST.toString().equals(methodType)
                || MethodType.PUT.toString().equals(methodType)) {
            if (sb.length() > 0) {
                if (bodyStr != null) {
                    bodyStr += "&";
                    bodyStr += sb.toString();
                } else {
                    bodyStr = sb.toString();
                }

            }
        }
        System.out.println("Calling URL	:	" + reqURL);
        System.out.println("Body		:	" + bodyStr);
        System.out.println("Header Params	:	" + headerParams);
        return HttpUtil.doHttpRequest(reqURL, methodType, bodyStr, headerParams);
    }

    @Override
    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public void setAccessTokenParameterName(String accessTokenParameterName) {
        this.accessTokenParameterName = accessTokenParameterName;
    }


}
