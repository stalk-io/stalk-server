package io.stalk.common.oauth.exception;

public class UserDeniedPermissionException extends Exception {

    private static final long serialVersionUID = 8089726143500613807L;

    public UserDeniedPermissionException() {
        super("User has denied permission to access his/her account");
    }

    /**
     * @param message
     */
    public UserDeniedPermissionException(final String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public UserDeniedPermissionException(final Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public UserDeniedPermissionException(final String message,
                                         final Throwable cause) {
        super(message, cause);
    }
}
