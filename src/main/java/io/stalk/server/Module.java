package io.stalk.server;

import org.vertx.java.core.json.JsonObject;

public class Module {

    private String      moduleName;
    private JsonObject  moduleConfig;
    private int         instances       = 1;
    private boolean     multiThreaded   = false;

    public Module(String moduleName) {
        this(moduleName, new JsonObject(), 1, false);
    }

    public Module(String moduleName, JsonObject moduleConfig) {
        this(moduleName, moduleConfig, 1, false);
    }

    public Module(String moduleName, JsonObject moduleConfig, int instances) {
        this(moduleName, moduleConfig, instances, false);
    }

    public Module(String moduleName, JsonObject moduleConfig, int instances, boolean multiThreaded) {
        this.moduleName     = moduleName;
        this.moduleConfig   = moduleConfig;
        this.instances      = instances;
        this.multiThreaded  = multiThreaded;
    }


    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public JsonObject getModuleConfig() {
        return moduleConfig;
    }

    public void setModuleConfig(JsonObject moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    public int getInstances() {
        return instances;
    }

    public void setInstances(int instances) {
        this.instances = instances;
    }

    public boolean isMultiThreaded() {
        return multiThreaded;
    }

    public void setMultiThreaded(boolean multiThreaded) {
        this.multiThreaded = multiThreaded;
    }

}
