package com.zzy.team;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zzy.team.mapper")
public class
TeamYuPaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeamYuPaoApplication.class, args);
    }

}
