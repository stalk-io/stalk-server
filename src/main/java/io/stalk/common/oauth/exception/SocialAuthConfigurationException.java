package io.stalk.common.oauth.exception;

public class SocialAuthConfigurationException extends Exception {

    private static final long serialVersionUID = 477153534655510364L;

    public SocialAuthConfigurationException() {
        super();
    }

    /**
     * @param message
     */
    public SocialAuthConfigurationException(final String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public SocialAuthConfigurationException(final Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public SocialAuthConfigurationException(final String message,
                                            final Throwable cause) {
        super(message, cause);
    }
}
