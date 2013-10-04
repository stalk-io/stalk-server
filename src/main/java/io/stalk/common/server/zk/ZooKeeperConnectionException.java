package io.stalk.common.server.zk;

public class ZooKeeperConnectionException extends Exception {

    private static final long serialVersionUID = 1678593460888622727L;

    public ZooKeeperConnectionException() {
        super();
    }

    public ZooKeeperConnectionException(final String message) {
        super(message);
    }

    public ZooKeeperConnectionException(final Throwable cause) {
        super(cause);
    }

    public ZooKeeperConnectionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
