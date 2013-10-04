package io.stalk.common.oauth.exception;

public class SocialAuthException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -6856436294292027306L;

    public SocialAuthException() {
        super();
    }

    public SocialAuthException(final String message) {
        super(message);
    }

    public SocialAuthException(final Throwable cause) {
        super(cause);
    }

    public SocialAuthException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
