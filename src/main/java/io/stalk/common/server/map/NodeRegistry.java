package io.stalk.common.server.map;

import java.util.HashMap;
import java.util.Set;


public class NodeRegistry<T> implements NodeMap<T> {

    private final HashMap<String, T> keyMap = new HashMap<String, T>();

    public NodeRegistry() {
    }

    @Override
    public void add(String key, T node) {
        keyMap.put(key, node);

    }

    @Override
    public void remove(String key) {
        keyMap.remove(key);
    }

    @Override
    public T get(String key) {
        return keyMap.get(key);
    }

    @Override
    public boolean isExist(String key) {
        return keyMap.containsKey(key);
    }

    @Override
    public Set<String> getKeys() {
        return keyMap.keySet();
    }

}