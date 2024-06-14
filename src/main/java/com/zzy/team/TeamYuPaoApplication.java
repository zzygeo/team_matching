package com.zzy.team;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.CrossOrigin;

@SpringBootApplication
@MapperScan("com.zzy.team.mapper")
@EnableScheduling
public class
TeamYuPaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeamYuPaoApplication.class, args);
    }

}
