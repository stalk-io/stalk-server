package io.stalk.common.oauth.exception;

public class SignatureException extends Exception {

    private static final long serialVersionUID = -3832456866408848000L;

    public SignatureException() {
        super();
    }

    /**
     * @param message
     */
    public SignatureException(final String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public SignatureException(final Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public SignatureException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
