package com.xcz.afcs.lock.impl.redis;

import com.xcz.afcs.lock.DistributedLock;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;


/**
 * Created by mac on 2017/8/24.
 */
public class RedisDistributedLockImpl implements DistributedLock {

    private static final long INTERVAL_SLEEP_MILL_TIME = 50;

    private static final long DEFAULT_EXPIRE_MILL_TIME = 5000;

    private Thread exclusiveOwnerThread = null;

    private final RedisTemplate redisTemplate;

    private String key;

    private long keyExpireMills;

    public RedisDistributedLockImpl(RedisTemplate redisTemplate, String key) {
        this(redisTemplate, key, DEFAULT_EXPIRE_MILL_TIME);
    }

    public RedisDistributedLockImpl(RedisTemplate redisTemplate, String key, long keyExpireMills) {
        this.redisTemplate = redisTemplate;
        this.key = key;
        this.keyExpireMills = keyExpireMills;
    }

    @Override
    public boolean tryLock(long waitLockMills) throws InterruptedException {
        if (waitLockMills < 1 || keyExpireMills < 1) {
            return false;
        }
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        Thread currentThread = Thread.currentThread();
        String threadId = String.valueOf(currentThread.getId());
        long timeout = waitLockMills;
        while (timeout >= 0) {
            //未被其他线程使用，占用此锁，并设置失效时间
            if (valueOperations.setIfAbsent(key, threadId)) {
                redisTemplate.expire(key, keyExpireMills, TimeUnit.MILLISECONDS);
                exclusiveOwnerThread = currentThread;
                return true;
            }
            //设置setnx后，reids crash 会导致expire未设置成功
            Long ttl = redisTemplate.getExpire(key);
            if (ttl == null || ttl < 0) {
                valueOperations.set(key, threadId, keyExpireMills, TimeUnit.MILLISECONDS);
                exclusiveOwnerThread = currentThread;
                return true;
            }
            Thread.sleep(INTERVAL_SLEEP_MILL_TIME);
            timeout -= INTERVAL_SLEEP_MILL_TIME;
        }
        return false;
    }

    @Override
    public boolean unLock() {
        if (exclusiveOwnerThread != null && exclusiveOwnerThread == Thread.currentThread()) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

}