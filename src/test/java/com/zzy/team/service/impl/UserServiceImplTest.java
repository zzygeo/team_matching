package com.zzy.team.service.impl;

import com.zzy.team.TeamYuPaoApplication;
import com.zzy.team.model.domain.User;
import com.zzy.team.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = {TeamYuPaoApplication.class})
class UserServiceImplTest {
    @Autowired
    UserService userService;

    private ThreadPoolExecutor pool = new ThreadPoolExecutor(60, 500, 10,TimeUnit.MINUTES, new ArrayBlockingQueue<>(1000));

    @Test
    public void testInsertUser() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int nums = 10000;
        ArrayList<User> users = new ArrayList<>();
        for (int i = 0; i < nums; i++) {
            User user = new User();
            user.setUsername("假用户");
            user.setUserAccount("fake zzy");
            user.setProfile("");
            user.setAvatarUrl("https://tse3-mm.cn.bing.net/th/id/OIP-C.e2oxwn3Qje4nPKAEBq4pkgHaEK?rs=1&pid=ImgDetMain");
            user.setUserPassword("12345678");
            user.setPhone("123");
            user.setEmail("123@qq.com");
            user.setUserStatus(0);
            user.setUserRole(0);
            user.setPlanetCode("1111");
            user.setTags("[]");
            user.setGender(0);
            users.add(user);
        }
        System.out.println("开始执行插入了");
        userService.saveBatch(users);
        stopWatch.stop();
        long l = stopWatch.getTotalTimeMillis();
        System.out.println(l);
    }

    @Test
    public void doConcurrentInsertUser() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        int batchSize = 500;
        // 分成10组，每组1000条
        int j = 0;
        for (int i = 0; i < 700; i++) {
            ArrayList<User> users = new ArrayList<>();
            while (true){
                j++;
                User user = new User();
                user.setUsername("假用户");
                user.setUserAccount("fake zzy");
                user.setProfile("");
                user.setAvatarUrl("https://tse3-mm.cn.bing.net/th/id/OIP-C.e2oxwn3Qje4nPKAEBq4pkgHaEK?rs=1&pid=ImgDetMain");
                user.setUserPassword("12345678");
                user.setPhone("123");
                user.setEmail("123@qq.com");
                user.setUserStatus(0);
                user.setUserRole(0);
                user.setPlanetCode("1111");
                user.setTags("[]");
                user.setGender(0);
                users.add(user);
                if (j % batchSize == 0) {
                    break;
                }
            }
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                System.out.println("threadName: " + Thread.currentThread().getName());
                userService.saveBatch(users, batchSize);
            }, pool);
            futureList.add(future);
        }
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        stopWatch.stop();
        long l = stopWatch.getTotalTimeMillis();
        System.out.println(l);
    }
}