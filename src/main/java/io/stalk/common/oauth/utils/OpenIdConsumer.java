package io.stalk.common.oauth.utils;

import io.stalk.common.oauth.Profile;

import java.util.HashMap;
import java.util.Map;


/**
 * @author tarunn@brickred.com
 */
public class OpenIdConsumer {

    private static Map<String, String> associationMap;
    private static Map<String, String> requestTokenMap;
    public static final String OPENID_REQUEST_TOKEN = "openid.ext2.request_token";

    static {
        associationMap = new HashMap<String, String>();
        associationMap.put("openid.ns", "http://specs.openid.net/auth/2.0");
        associationMap.put("openid.mode", "associate");
        associationMap.put("openid.assoc_type", "HMAC-SHA1");
        associationMap.put("openid.session_type", "no-encryption");

        requestTokenMap = new HashMap<String, String>();
        requestTokenMap.put("openid.ns", "http://specs.openid.net/auth/2.0");
        requestTokenMap.put("openid.claimed_id",
                "http://specs.openid.net/auth/2.0/identifier_select");
        requestTokenMap.put("openid.identity",
                "http://specs.openid.net/auth/2.0/identifier_select");
        requestTokenMap.put("openid.mode", "checkid_setup");
        requestTokenMap.put("openid.ns.pape",
                "http://specs.openid.net/extensions/pape/1.0");
        requestTokenMap.put("openid.ns.max_auth_age", "0");
        requestTokenMap.put("openid.ns.ax", "http://openid.net/srv/ax/1.0");
        requestTokenMap.put("openid.ax.mode", "fetch_request");
        requestTokenMap.put("openid.ax.type.country",
                "http://axschema.org/contact/country/home");
        requestTokenMap.put("openid.ax.type.email",
                "http://axschema.org/contact/email");
        requestTokenMap.put("openid.ax.type.firstname",
                "http://axschema.org/namePerson/first");
        requestTokenMap.put("openid.ax.type.language",
                "http://axschema.org/pref/language");
        requestTokenMap.put("openid.ax.type.lastname",
                "http://axschema.org/namePerson/last");
        requestTokenMap.put("openid.ax.required",
                "country,email,firstname,language,lastname");
        // ADDING OAUTH PROTOCOLS
        requestTokenMap.put("openid.ns.ext2",
                "http://specs.openid.net/extensions/oauth/1.0");

    }

    /**
     * Provides an association URL. An association between the Relying Party and
     * the OpenID Provider establishes a shared secret between them, which is
     * used to verify subsequent protocol messages and reduce round trips.
     *
     * @param url
     * @return Association URL
     * @throws Exception
     */
    public static String getAssociationURL(final String url) throws Exception {
        String param = HttpUtil.buildParams(associationMap);
        char separator = url.indexOf('?') == -1 ? '?' : '&';
        return url + separator + param;
    }

    /**
     * It obtains the request token. The Request Token is a temporary token used
     * to initiate User authorization.
     *
     * @param requestTokenUrl the Request Token URL
     * @param returnTo        Callback URL
     * @param realm           Realm
     * @param assocHandle
     * @param consumerURL
     * @param scope
     * @return Generated Request Token URL
     * @throws Exception
     */
    public static String getRequestTokenURL(final String requestTokenUrl,
                                            final String returnTo, final String realm,
                                            final String assocHandle, final String consumerURL,
                                            final String scope) throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.putAll(requestTokenMap);
        params.put("openid.return_to", returnTo);
        params.put("openid.realm", realm);
        params.put("openid.assoc_handle", assocHandle);
        params.put("openid.ext2.consumer", consumerURL);
        if (scope != null) {
            params.put("openid.ext2.scope", scope);
        }
        String paramStr = HttpUtil.buildParams(params);
        char separator = requestTokenUrl.indexOf('?') == -1 ? '?' : '&';
        String url = requestTokenUrl + separator + paramStr;
        System.out.println("Request Token URL : " + url);
        return url;
    }

    /**
     * Parses the user info from request
     *
     * @param requestParams request parameters map
     * @return User Profile
     */
    public static Profile getUserInfo(final Map<String, String> requestParams) {
        Profile p = new Profile();

        //p.setLink(requestParams.get("openid.ext1.value.email"))

        System.out.println(requestParams); // YOHAN
        /*
        p.setEmail(requestParams.get("openid.ext1.value.email"));
		p.setFirstName(requestParams.get("openid.ext1.value.firstname"));
		p.setLastName(requestParams.get("openid.ext1.value.lastname"));
		p.setCountry(requestParams.get("openid.ext1.value.country"));
		p.setLanguage(requestParams.get("openid.ext1.value.language"));
		p.setValidatedId(requestParams.get("openid.identity"));
		*/
        return p;
    }
}
