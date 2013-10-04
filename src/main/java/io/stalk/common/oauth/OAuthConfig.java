package io.stalk.common.oauth;

import io.stalk.common.oauth.utils.Constants;
import io.stalk.common.oauth.utils.MethodType;

public class OAuthConfig {

    private final String _consumerKey;
    private final String _consumerSecret;
    private final String _callbackUrl;
    private final String _signatureMethod;
    private final String _transportName;
    private String id;
    private Class<?> providerImplClass;


    public OAuthConfig(final String consumerKey, final String consumerSecret, final String callbackUrl,
                       final String signatureMethod, final String transportName) {
        _consumerKey = consumerKey;
        _consumerSecret = consumerSecret;
        if (signatureMethod == null || signatureMethod.length() == 0) {
            _signatureMethod = Constants.HMACSHA1_SIGNATURE;
        } else {
            _signatureMethod = signatureMethod;
        }
        if (transportName == null || transportName.length() == 0) {
            _transportName = MethodType.GET.toString();
        } else {
            _transportName = transportName;
        }
        if (callbackUrl == null || callbackUrl.length() == 0) {
            _callbackUrl = "";
        } else {
            _callbackUrl = callbackUrl;
        }

    }

    public OAuthConfig(final String consumerKey, final String consumerSecret, final String callbackUrl) {
        _consumerKey = consumerKey;
        _consumerSecret = consumerSecret;
        _transportName = MethodType.GET.toString();
        _signatureMethod = Constants.HMACSHA1_SIGNATURE;


        if (callbackUrl == null || callbackUrl.length() == 0) {
            _callbackUrl = "";
        } else {
            _callbackUrl = callbackUrl;
        }
    }

    public String get_consumerKey() {
        return _consumerKey;
    }

    public String get_consumerSecret() {
        return _consumerSecret;
    }

    public String get_callbackUrl() {
        return _callbackUrl;
    }

    public String get_signatureMethod() {
        return _signatureMethod;
    }

    public String get_transportName() {
        return _transportName;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public Class<?> getProviderImplClass() {
        return providerImplClass;
    }

    public void setProviderImplClass(final Class<?> providerImplClass) {
        this.providerImplClass = providerImplClass;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");
        result.append(this.getClass().getName() + " Object {" + NEW_LINE);
        result.append(" consumerKey: " + _consumerKey + NEW_LINE);
        result.append(" consumerSecret: " + _consumerSecret + NEW_LINE);
        result.append(" signatureMethod: " + _signatureMethod + NEW_LINE);
        result.append(" transportName: " + _transportName + NEW_LINE);
        result.append(" id: " + id + NEW_LINE);
        result.append(" providerImplClass: " + providerImplClass + NEW_LINE);
        result.append("}");
        return result.toString();
    }


}
