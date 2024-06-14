package com.zzy.team.service.impl;

import com.zzy.team.TeamYuPaoApplication;
import com.zzy.team.model.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * 测试redis的方法
 */

@SpringBootTest(classes = {TeamYuPaoApplication.class})
public class RedisTest {
    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void test() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set("name", "zzy");
        valueOperations.set("age", 18);
        valueOperations.set("money", 10000.0);
        User user = new User();
        user.setUserAccount("zzygeo");
        user.setUsername("大表哥");
        valueOperations.set("user", user);

        // search

        System.out.println(valueOperations.get("name"));

        System.out.println(valueOperations.get("age"));

        System.out.println(valueOperations.get("money"));

        System.out.println(valueOperations.get("user"));
    }

}
