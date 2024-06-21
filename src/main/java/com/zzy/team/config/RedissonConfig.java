package com.zzy.team.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;

/**
 * redisson配置
 */

@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {

    private String host;
    private String port;
    private String password;

    @Bean
    public RedissonClient redissonClient() throws IOException {
        Config config = new Config();
        String redisAddress = String.format("redis://%s:%s", host, port);
        System.out.println("redisson: " + redisAddress);
        // use "rediss://" for SSL connection
        config.useSingleServer().setAddress(redisAddress)
                .setDatabase(1).setPassword(password);

        // create instance
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}
