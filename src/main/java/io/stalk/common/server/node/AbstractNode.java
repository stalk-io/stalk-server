package io.stalk.common.server.node;

import org.vertx.java.core.logging.Logger;

public abstract class AbstractNode {

    private Logger log;
    private String prefix;

    public AbstractNode(Logger log, String prefix) {
        this.log = log;
        this.prefix = prefix;
    }

    protected void DEBUG(String message, Object... args) {
        if (log != null) log.debug("[NODE::" + prefix + "] " + String.format(message, args));
    }

    protected void ERROR(String message, Object... args) {
        if (log != null) log.error("[NODE::" + prefix + "] " + String.format(message, args));
    }

}
