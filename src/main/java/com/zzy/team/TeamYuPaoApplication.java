package com.zzy.team;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.TimeZone;

@SpringBootApplication
@MapperScan("com.zzy.team.service.mapper")
@EnableScheduling
public class
TeamYuPaoApplication {
    public static void main(String[] args) {
        SpringApplication.run(TeamYuPaoApplication.class, args);
    }
}
