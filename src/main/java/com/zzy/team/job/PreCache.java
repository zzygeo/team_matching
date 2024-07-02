package com.zzy.team.job;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzy.team.constant.UserConstant;
import com.zzy.team.model.domain.User;
import com.zzy.team.service.UserService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 缓存预热
 */
@Component
public class PreCache {

    @Autowired
    private RedisTemplate redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Autowired
    private UserService userService;

    /**
     * 重点用户
     */
    private List<Long> userIds = List.of(1L);

    @Scheduled(cron = "0 0 0 * * ?")
    public void preCache() {
        // 产生竞争，使用分布式锁保证只有部分机器可以执行以下的语句
        RLock lock = redissonClient.getLock(UserConstant.PRECACHE_JOB_LOCK);
        try {
            // 不等待，如果拿不到锁就直接走，保证只有一台机器可以执行这个定时任务
            if (lock.tryLock(0l, 30000l, TimeUnit.MILLISECONDS)) {
                System.out.println("get lock: " + Thread.currentThread().getId());
                Page<User> userPage = userService.page(new Page<>(1, 10));
                List<User> users = userPage.getRecords().stream().map(item -> userService.getSafeUser(item)).collect(Collectors.toList());
                userPage.setRecords(users);
                for (Long userId : userIds) {
                    String user_key = UserConstant.RECOMMEND_USER + userId + ":1";
                    redisTemplate.opsForValue().set(user_key, userPage);
                }
                // 释放锁的时候 要判断是不是自己加的锁
                // 如果有新的任务来执行，拿不到锁的话，给锁续期，但是在这个场景下是不需要的
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
