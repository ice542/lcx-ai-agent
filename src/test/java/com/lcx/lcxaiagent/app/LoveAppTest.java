package com.lcx.lcxaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class LoveAppTest {
    @Resource
    private LoveApp loveApp;

    @Test
    void doChat() {
        String chatId= UUID.randomUUID().toString();
        //第一轮
        String message="你好，我是布克粉丝";
        String answer= loveApp.doChat(message,chatId);
        //第二轮
        message="我想找个女朋友";
        answer= loveApp.doChat(message,chatId);
        Assertions.assertNotNull(answer);
        //第三轮
        message="我刚刚问你什么问题";
        answer= loveApp.doChat(message,chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithReport() {
        String chatId= UUID.randomUUID().toString();
        //第一轮
        String message="你好，我是NBA球星布克小迷弟，我想找一个女朋友，但我不知道该怎么做";
        LoveApp.LoveReport loveReport=loveApp.doChatWithReport(message,chatId);
        Assertions.assertNotNull(loveReport);
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "恋爱中吵架怎么办";
        String answer =  loveApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }
}