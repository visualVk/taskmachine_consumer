package org.wwx.client.lock;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

/**
 * @author visualvk
 * @data 2023/9/25
 * @apiNote
 */
public class RedissonLock {

    private static class Holder {
        private static final RedissonClient INSTANCE = createClient();

        private static RedissonClient createClient(){
            Config config = new Config();
            config.useSingleServer()
                    .setAddress("redis://42.192.211.121:32008");
            return Redisson.create(config);
        }
    }

    public static RedissonClient getRedissonClient() {
        return Holder.INSTANCE;
    }
}
