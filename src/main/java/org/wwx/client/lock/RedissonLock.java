package org.wwx.client.lock;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.wwx.client.constant.LockConfig;

public class RedissonLock {

    private static class Holder {
        private static final RedissonClient INSTANCE = createClient();

        private static RedissonClient createClient(){
            Config config = new Config();
            String redisAddress = "redis://" + LockConfig.REDIS_URL;
            config.useSingleServer()
                    .setAddress(redisAddress);
            return Redisson.create(config);
        }
    }

    public static RedissonClient getRedissonClient() {
        return Holder.INSTANCE;
    }
}
