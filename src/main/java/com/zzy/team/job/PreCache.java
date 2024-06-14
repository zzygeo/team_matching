package com.zzy.team.job;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzy.team.constant.UserConstant;
import com.zzy.team.model.domain.User;
import com.zzy.team.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 缓存预热
 */
@Component
public class PreCache {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void preCache() {
        Page<User> userPage = userService.pageUsers(1, 10, null);
        List<User> users = userPage.getRecords().stream().map(item -> userService.getSafeUser(item)).collect(Collectors.toList());
        userPage.setRecords(users);
        String user_key = UserConstant.RECOMMEND_USER + 1;
        redisTemplate.opsForValue().set(user_key, userPage);
    }



}
