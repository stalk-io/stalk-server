package io.stalk.common.server.zk;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.ACL;
import org.vertx.java.core.json.JsonObject;

import java.util.List;
import java.util.logging.Logger;

public final class ZooKeeperUtils {

    private static final Logger LOG = Logger.getLogger(ZooKeeperUtils.class.getName());

    public static final int DEFAULT_ZK_SESSION_TIMEOUT = 1000;

    public static final int ANY_VERSION = -1;

    public static final ImmutableList<ACL> EVERYONE_READ_CREATOR_ALL =
            ImmutableList.<ACL>builder()
                    .addAll(Ids.CREATOR_ALL_ACL)
                    .addAll(Ids.READ_ACL_UNSAFE)
                    .build();

    public static boolean isRetryable(KeeperException e) {
        Preconditions.checkNotNull(e);

        switch (e.code()) {
            case CONNECTIONLOSS:
            case SESSIONEXPIRED:
            case SESSIONMOVED:
            case OPERATIONTIMEOUT:
                return true;

            case RUNTIMEINCONSISTENCY:
            case DATAINCONSISTENCY:
            case MARSHALLINGERROR:
            case BADARGUMENTS:
            case NONODE:
            case NOAUTH:
            case BADVERSION:
            case NOCHILDRENFOREPHEMERALS:
            case NODEEXISTS:
            case NOTEMPTY:
            case INVALIDCALLBACK:
            case INVALIDACL:
            case AUTHFAILED:
            case UNIMPLEMENTED:

                // These two should not be encountered - they are used internally by ZK to specify ranges
            case SYSTEMERROR:
            case APIERROR:

            case OK: // This is actually an invalid ZK exception code

            default:
                return false;
        }
    }

    public static void ensurePath(ZooKeeperClient zkClient, List<ACL> acl, String path)
            throws ZooKeeperConnectionException, InterruptedException, KeeperException {
        Preconditions.checkNotNull(zkClient);
        Preconditions.checkNotNull(path);
        Preconditions.checkArgument(path.startsWith("/"));

        ensurePathInternal(zkClient, acl, path);
    }

    private static void ensurePathInternal(ZooKeeperClient zkClient, List<ACL> acl, String path)
            throws ZooKeeperConnectionException, InterruptedException, KeeperException {
        if (zkClient.get().exists(path, false) == null) {
            // The current path does not exist; so back up a level and ensure the parent path exists
            // unless we're already a root-level path.
            int lastPathIndex = path.lastIndexOf('/');
            if (lastPathIndex > 0) {
                ensurePathInternal(zkClient, acl, path.substring(0, lastPathIndex));
            }

            // We've ensured our parent path (if any) exists so we can proceed to create our path.
            try {
                zkClient.get().create(path, null, acl, CreateMode.PERSISTENT);
            } catch (KeeperException.NodeExistsException e) {
                // This ensures we don't die if a race condition was met between checking existence and
                // trying to create the node.
                LOG.info("Node existed when trying to ensure path " + path + ", somebody beat us to it?");
            }
        }
    }


    public static String createServerNode(
            ZooKeeperClient zkClient,
            List<ACL> acl,
            String rootPath,
            String path,
            JsonObject data) throws ZooKeeperConnectionException, InterruptedException, KeeperException {

        ensurePath(zkClient, acl, rootPath);

        return zkClient.get().create(
                rootPath + "/" + path,
                data.encode().getBytes(),
                acl,
                CreateMode.EPHEMERAL);
    }


    private ZooKeeperUtils() {
        // utility
    }
}
