package io.stalk.common.server.node;

import org.apache.commons.lang.StringUtils;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Set;

public class RedisPoolNode extends AbstractNode {

    private String channel;
    private JedisPool jedisPool;

    public RedisPoolNode(JsonObject jsonObject, Logger log, String prefix) {

        super(log, prefix);

        this.channel = jsonObject.getString("channel");

        JedisPoolConfig config = new JedisPoolConfig();
        config.testOnBorrow = true;

        JedisPool jedisPool;

        if (StringUtils.isEmpty(jsonObject.getString("host"))) {
            jedisPool = new JedisPool(config, "localhost");
        } else {
            jedisPool = new JedisPool(config,
                    jsonObject.getString("host"),
                    jsonObject.getInteger("port").intValue());
        }

        this.jedisPool = jedisPool;


        Jedis jedis = this.jedisPool.getResource();
        DEBUG("connected <%s!!> %s", jedis.ping(), jsonObject);
        this.jedisPool.returnResource(jedis);
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public Long hset(String key, String field, String value) {
        Jedis jedis = jedisPool.getResource();
        Long status = jedis.hset(key, field, value);
        jedisPool.returnResource(jedis);

        return status;
    }

    public String hget(String key, String field) {
        Jedis jedis = jedisPool.getResource();
        String value = jedis.hget(key, field);
        jedisPool.returnResource(jedis);

        return value;
    }

    public Set<String> hkeys(String key) {
        Jedis jedis = jedisPool.getResource();
        Set<String> keys = jedis.hkeys(key);
        jedisPool.returnResource(jedis);

        return keys;
    }

    public Long hdel(String key, String... field) {
        Jedis jedis = jedisPool.getResource();
        Long status = jedis.hdel(key, field);
        jedisPool.returnResource(jedis);

        return status;
    }

    public String set(String key, String value) {
        Jedis jedis = jedisPool.getResource();
        String returnVal = jedis.set(key, value);
        jedisPool.returnResource(jedis);

        return returnVal;
    }

    public String get(String key) {
        Jedis jedis = jedisPool.getResource();
        String value = jedis.get(key);
        jedisPool.returnResource(jedis);

        return value;
    }

    public Long publish(String channel, String message) {
        Jedis jedis = jedisPool.getResource();
        Long status = jedis.publish(channel, message);
        jedisPool.returnResource(jedis);

        return status;
    }

}
