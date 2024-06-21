package com.zzy.team.service.impl;

import com.zzy.team.constant.UserConstant;
import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class RedissonTest {
    // 默认按照byName去查找，找不到则按照byType去查找，如果指定了name，则不会按照byType去找
    @Resource
    private RedissonClient redissonClient;

//    @Test
    public void test() {
        // 像操作java一样操作redis
        // list
        RList<String> rList = redissonClient.getList("test-list");
        rList.add("zzy");
        rList.add("tyt");
        boolean isRemove = rList.remove("zzy");
        System.out.println("isRemove: " + isRemove);
    }

//    @Test
    public void testDog() {
        // 产生竞争，使用分布式锁保证只有部分机器可以执行以下的语句
        RLock lock = redissonClient.getLock(UserConstant.PRECACHE_JOB_LOCK);
        try {
            // 不等待，如果拿不到锁就直接走，保证只有一台机器可以执行这个定时任务
            if (lock.tryLock(0l, 0l, TimeUnit.MILLISECONDS)) {
                System.out.println("get lock: " + Thread.currentThread().getId());
                Thread.sleep(300000);
                // 释放锁的时候 要判断是不是自己加的锁
            } else {
                System.out.println("分布式锁获取失败");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unlock");
                lock.unlock();
            }
        }
    }
}
