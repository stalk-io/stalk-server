package io.stalk.common.server.map;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.util.HashMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;


public class ConsistentHash<T> implements NodeMap<T> {

    public static final int NUMBER_OF_REPLICAS = 100;

    private final HashMap<String, Long[]> keyMap = new HashMap<String, Long[]>();

    private final HashFunction hashFunction;
    private final int numberOfReplicas;
    private final SortedMap<Long, T> circle = new TreeMap<Long, T>();

    public ConsistentHash() {
        this(Hashing.md5(), NUMBER_OF_REPLICAS);
    }

    public ConsistentHash(int numberOfReplicas) {
        this(Hashing.md5(), numberOfReplicas);
    }

    public ConsistentHash(HashFunction hashFunction, int numberOfReplicas) {
        this.hashFunction = hashFunction;
        this.numberOfReplicas = numberOfReplicas;
    }

    public void add(String key, T node) {
        Long keyArray[] = new Long[numberOfReplicas];
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.put(hashFunction.hashString(key + i).asLong(),
                    node);
            keyArray[i] = hashFunction.hashString(key + i).asLong();
        }
        keyMap.put(key, keyArray);
    }

    public void remove(String key) {

        for (int i = 0; i < numberOfReplicas; i++) {
            circle.remove(hashFunction.hashString(key + i).asLong());
        }
        keyMap.remove(key);
    }

    public boolean isExist(String key) {
        return keyMap.containsKey(key);
    }

    public Set<String> getKeys() {
        return keyMap.keySet();
    }

    public T get(String key) {
        if (circle.isEmpty()) {
            return null;
        }
        long hash = hashFunction.hashString(key).asLong();
        if (!circle.containsKey(hash)) {
            SortedMap<Long, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }


}