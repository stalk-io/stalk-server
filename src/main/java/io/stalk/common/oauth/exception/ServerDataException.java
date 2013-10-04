package io.stalk.common.oauth.exception;

public class ServerDataException extends Exception {

    private static final long serialVersionUID = 7313004590704350522L;

    public ServerDataException() {
        super();
    }

    /**
     * @param message
     */
    public ServerDataException(final String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public ServerDataException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public ServerDataException(final Throwable cause) {
        super(cause);
    }
}
