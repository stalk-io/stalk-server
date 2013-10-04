package io.stalk.common.server.zk;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.SessionExpiredException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class ZooKeeperClient {


    private static final Logger LOG = Logger.getLogger(ZooKeeperClient.class.getName());

    private static final Long WAIT_FOREVER = 0L;

    private final int sessionTimeoutMs;
    private final Credentials credentials;
    private final String zooKeeperServers;
    private volatile ZooKeeper zooKeeper;
    private SessionState sessionState;

    private final Set<Watcher> watchers = Collections.synchronizedSet(new HashSet<Watcher>());

    public ZooKeeperClient(int sessionTimeout, JsonArray zooKeeperServers) throws ZooKeeperConnectionException {
        this(sessionTimeout, Credentials.NONE, zooKeeperServers);
    }

    public ZooKeeperClient(int sessionTimeout, Credentials credentials, JsonArray zooKeeperServers) throws ZooKeeperConnectionException {
        this.sessionTimeoutMs = sessionTimeout;
        this.credentials = credentials;

        if (zooKeeperServers == null) {
            throw new ZooKeeperConnectionException("Zookeeper configuratino is not valid.");
        }

        StringBuffer sbServer = new StringBuffer();
        ;
        int i = 0;
        for (Object zkInfo : zooKeeperServers) {
            if (!(zkInfo instanceof JsonObject)) {
                throw new IllegalArgumentException("Permitted must only contain JsonObject");
            }
            JsonObject jsonJkConfig = (JsonObject) zkInfo;

            if (i > 0) sbServer.append(",");
            sbServer = sbServer.
                    append(jsonJkConfig.getString("host")).
                    append(":").
                    append(jsonJkConfig.getString("port"));

            i = i + 1;

        }
        this.zooKeeperServers = sbServer.toString();

    }

    public synchronized ZooKeeper get() throws ZooKeeperConnectionException, InterruptedException {
        try {
            return get(WAIT_FOREVER);
        } catch (TimeoutException e) {
            InterruptedException interruptedException = new InterruptedException("Got an unexpected TimeoutException for 0 wait");
            interruptedException.initCause(e);
            throw interruptedException;
        }
    }

    public synchronized ZooKeeper get(Long connectionTimeout) throws ZooKeeperConnectionException, InterruptedException, TimeoutException {

        if (zooKeeper == null) {
            final CountDownLatch connected = new CountDownLatch(1);
            Watcher watcher = new Watcher() {
                public void process(WatchedEvent event) {
                    switch (event.getType()) {
                        case None:
                            switch (event.getState()) {
                                case Expired:
                                    LOG.info("[ZooKeeperClient] Zookeeper session expired. Event: " + event);
                                    //close();
                                    break;
                                case SyncConnected:
                                    LOG.info("[ZooKeeperClient] Event: " + event);
                                    connected.countDown();
                                    break;
                                default:
                                    // do nothing.
                            }
                        default:
                            // do nothing.
                    }

                    synchronized (watchers) {
                        for (Watcher watcher : watchers) {
                            watcher.process(event);
                        }
                    }
                }
            };

            try {
                zooKeeper = (sessionState != null)
                        ? new ZooKeeper(zooKeeperServers, sessionTimeoutMs, watcher, sessionState.sessionId,
                        sessionState.sessionPasswd)
                        : new ZooKeeper(zooKeeperServers, sessionTimeoutMs, watcher);
            } catch (IOException e) {
                LOG.warning("[ZooKeeperClient] Problem connecting to servers: " + zooKeeperServers);
                throw new ZooKeeperConnectionException(
                        "Problem connecting to servers: " + zooKeeperServers, e);
            }

            if (connectionTimeout > 0) {
                if (!connected.await(connectionTimeout, TimeUnit.MILLISECONDS)) {
                    close();
                    throw new TimeoutException("Timed out waiting for a ZK connection after " + connectionTimeout);
                }
            } else {
                try {
                    connected.await();
                } catch (InterruptedException ex) {
                    LOG.info("[ZooKeeperClient] Interrupted while waiting to connect to zooKeeper");
                    close();
                    throw ex;
                }
            }
            credentials.authenticate(zooKeeper);

            sessionState = new SessionState(zooKeeper.getSessionId(), zooKeeper.getSessionPasswd());
            LOG.info("[ZooKeeperClient] Zookeeper is connected. (id:" + sessionState.sessionId + ")");
        }
        return zooKeeper;
    }

    public void register(Watcher watcher) {
        watchers.add(watcher);
    }

    public boolean unregister(Watcher watcher) {
        return watchers.remove(watcher);
    }

    public boolean shouldRetry(KeeperException e) {
        if (e instanceof SessionExpiredException) {
            close();
        }
        return ZooKeeperUtils.isRetryable(e);
    }

    public synchronized void close() {
        if (zooKeeper != null) {
            try {
                zooKeeper.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warning("Interrupted trying to close zooKeeper");
            } finally {
                zooKeeper = null;
                sessionState = null;
            }
        }
    }

    public interface Credentials {
        void authenticate(ZooKeeper zooKeeper);

        String scheme();

        byte[] authToken();

        Credentials NONE = new Credentials() {
            public void authenticate(ZooKeeper zooKeeper) {
            }

            public String scheme() {
                return null;
            }

            public byte[] authToken() {
                return null;
            }
        };

    }

    public static Credentials digestCredentials(String username, String password) {
        // digest authentication mechanism.
        return credentials("digest", (username + ":" + password).getBytes());
    }

    public static Credentials credentials(final String scheme, final byte[] authToken) {
        return new Credentials() {
            public void authenticate(ZooKeeper zooKeeper) {
                zooKeeper.addAuthInfo(scheme, authToken);
            }

            public String scheme() {
                return scheme;
            }

            public byte[] authToken() {
                return authToken;
            }
        };
    }

    private final class SessionState {
        private final long sessionId;
        private final byte[] sessionPasswd;

        private SessionState(long sessionId, byte[] sessionPasswd) {
            this.sessionId = sessionId;
            this.sessionPasswd = sessionPasswd;
        }
    }


}
