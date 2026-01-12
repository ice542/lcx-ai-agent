package com.lcx.lcxaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class TestAppTest {
    @Resource
    private TestApp testApp;

    @Test
    void dochat() {
        //第一轮
        String message="我是一名游戏爱好者";
        String chatId= UUID.randomUUID().toString();
        String answer=testApp.dochat(message,chatId);
        //第二轮
        message="能不能推荐一款游戏给我";
        answer= testApp.dochat(message,chatId);
        Assertions.assertNotNull(answer);
        //第三轮
        message="我刚刚问你什么问题";
        answer= testApp.dochat(message,chatId);
        Assertions.assertNotNull(answer);

    }
}