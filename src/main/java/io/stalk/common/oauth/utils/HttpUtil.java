package io.stalk.common.oauth.utils;

import io.stalk.common.oauth.exception.SocialAuthException;

import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HttpUtil {

    private static int timeoutValue = 0;

    public static void setConnectionTimeout(final int timeout) {
        timeoutValue = timeout;
    }

    public static String encodeURIComponent(final String value)
            throws Exception {
        if (value == null) {
            return "";
        }

        try {
            return URLEncoder.encode(value, "utf-8")
                    // OAuth encodes some characters differently:
                    .replace("+", "%20").replace("*", "%2A")
                    .replace("%7E", "~");
            // This could be done faster with more hand-crafted code.
        } catch (UnsupportedEncodingException wow) {
            throw new SocialAuthException(wow.getMessage(), wow);
        }
    }

    public static String decodeURIComponent(final String encodedURI) {
        char actualChar;

        StringBuffer buffer = new StringBuffer();

        int bytePattern, sumb = 0;

        for (int i = 0, more = -1; i < encodedURI.length(); i++) {
            actualChar = encodedURI.charAt(i);

            switch (actualChar) {
                case '%': {
                    actualChar = encodedURI.charAt(++i);
                    int hb = (Character.isDigit(actualChar) ? actualChar - '0'
                            : 10 + Character.toLowerCase(actualChar) - 'a') & 0xF;
                    actualChar = encodedURI.charAt(++i);
                    int lb = (Character.isDigit(actualChar) ? actualChar - '0'
                            : 10 + Character.toLowerCase(actualChar) - 'a') & 0xF;
                    bytePattern = (hb << 4) | lb;
                    break;
                }
                case '+': {
                    bytePattern = ' ';
                    break;
                }
                default: {
                    bytePattern = actualChar;
                }
            }

            if ((bytePattern & 0xc0) == 0x80) { // 10xxxxxx
                sumb = (sumb << 6) | (bytePattern & 0x3f);
                if (--more == 0) {
                    buffer.append((char) sumb);
                }
            } else if ((bytePattern & 0x80) == 0x00) { // 0xxxxxxx
                buffer.append((char) bytePattern);
            } else if ((bytePattern & 0xe0) == 0xc0) { // 110xxxxx
                sumb = bytePattern & 0x1f;
                more = 1;
            } else if ((bytePattern & 0xf0) == 0xe0) { // 1110xxxx
                sumb = bytePattern & 0x0f;
                more = 2;
            } else if ((bytePattern & 0xf8) == 0xf0) { // 11110xxx
                sumb = bytePattern & 0x07;
                more = 3;
            } else if ((bytePattern & 0xfc) == 0xf8) { // 111110xx
                sumb = bytePattern & 0x03;
                more = 4;
            } else { // 1111110x
                sumb = bytePattern & 0x01;
                more = 5;
            }
        }
        return buffer.toString();
    }

    public static Response doHttpRequest(
            final String urlStr,
            final String requestMethod,
            final String body,
            final Map<String, String> header) throws Exception {

        HttpURLConnection conn;

        try {

            URL url = new URL(urlStr);
            //if (proxyObj != null) {
            //	conn = (HttpURLConnection) url.openConnection(proxyObj);
            //} else {
            conn = (HttpURLConnection) url.openConnection();
            //}

            if (requestMethod.equalsIgnoreCase(MethodType.POST.toString())
                    || requestMethod
                    .equalsIgnoreCase(MethodType.PUT.toString())) {
                conn.setDoOutput(true);
            }

            conn.setDoInput(true);

            conn.setInstanceFollowRedirects(true);
            if (timeoutValue > 0) {
                conn.setConnectTimeout(timeoutValue);
            }
            if (requestMethod != null) {
                conn.setRequestMethod(requestMethod);
            }
            if (header != null) {
                for (String key : header.keySet()) {
                    conn.setRequestProperty(key, header.get(key));
                }
            }

            // If use POST or PUT must use this
            OutputStreamWriter wr = null;
            if (body != null) {
                if (requestMethod != null
                        && !MethodType.GET.toString().equals(requestMethod)
                        && !MethodType.DELETE.toString().equals(requestMethod)) {
                    wr = new OutputStreamWriter(conn.getOutputStream());
                    wr.write(body);
                    wr.flush();
                }
            }
            conn.connect();
        } catch (Exception e) {
            throw new SocialAuthException(e);
        }

        return new Response(conn);

    }

    public static String buildParams(final Map<String, String> params)
            throws Exception {
        List<String> argList = new ArrayList<String>();

        for (String key : params.keySet()) {
            String val = params.get(key);
            if (val != null && val.length() > 0) {
                String arg = key + "=" + encodeURIComponent(val);
                argList.add(arg);
            }
        }
        Collections.sort(argList);
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < argList.size(); i++) {
            s.append(argList.get(i));
            if (i != argList.size() - 1) {
                s.append("&");
            }
        }
        return s.toString();
    }


}
