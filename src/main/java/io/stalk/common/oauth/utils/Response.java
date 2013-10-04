package io.stalk.common.oauth.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.zip.GZIPInputStream;

/**
 * Encapsulates the HTTP status, headers and the content.
 *
 * @author tarunn@brickred.com
 */
public class Response {

    private final HttpURLConnection _connection;

    Response(final HttpURLConnection connection) {
        _connection = connection;
    }

    /**
     * Closes the connection
     *
     * @throws java.io.IOException
     */
    public void close() throws IOException {
        _connection.disconnect();
    }

    public String getHeader(final String name) {
        return _connection.getHeaderField(name);
    }

    /**
     * Gets the response content via InputStream.
     *
     * @return response input stream
     * @throws java.io.IOException
     */
    public InputStream getInputStream() throws IOException {
        return _connection.getInputStream();
    }

    /**
     * Gets the response HTTP status.
     *
     * @return the HTTP status
     */
    public int getStatus() {
        try {
            return _connection.getResponseCode();
        } catch (IOException e) {
            return 404;
        }
    }

    /**
     * Gets the response content as String using given encoding
     *
     * @param encoding the encoding type
     * @return Response body
     * @throws Exception
     */
    public String getResponseBodyAsString(final String encoding)
            throws Exception {
        String line = null;
        BufferedReader reader = null;
        StringBuffer sb = new StringBuffer();

        if (Constants.GZIP_CONTENT_ENCODING.equals(_connection
                .getHeaderField(Constants.CONTENT_ENCODING_HEADER))) {

            reader = new BufferedReader(
                    new InputStreamReader(
                            new GZIPInputStream(_connection.getInputStream()), encoding));
        } else {
            reader = new BufferedReader(new InputStreamReader(_connection.getInputStream(), encoding));
        }
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
}
