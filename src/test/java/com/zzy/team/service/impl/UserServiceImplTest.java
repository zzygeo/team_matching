package com.zzy.team.service.impl;

import com.zzy.team.TeamYuPaoApplication;
import com.zzy.team.model.domain.User;
import com.zzy.team.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = {TeamYuPaoApplication.class})
class UserServiceImplTest {
    @Autowired
    UserService userService;

    @Test
    void searchUserByTags() {
        List<User> users = userService.searchUserByTags(List.of("java", "c++"));
        System.out.println(users);
    }
}