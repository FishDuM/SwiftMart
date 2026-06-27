package hk.ljx.swiftmart.order.utils;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderLockUtils {
    private final ConcurrentHashMap<String, Boolean> lockMap = new ConcurrentHashMap<>();

    /**
     * 尝试加锁
     * @param key
     * @return
     */
    public boolean tryLock(String key) {
        return lockMap.putIfAbsent(key, Boolean.TRUE) == null;
    }

    /**
     * 释放锁
     * @param key
     */
    public void unlock(String key) {
        lockMap.remove(key);
    }
}
