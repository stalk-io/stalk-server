package io.stalk.common.server.map;

import java.util.Set;


public interface NodeMap<T> {

    public void add(String key, T node);

    public void remove(String key);

    public T get(String key);

    public boolean isExist(String key);

    public Set<String> getKeys();

}