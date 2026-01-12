package com.lcx.lcxaiagent.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TestApp {
    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT="你是一名游戏教练，可以教用户怎么打游戏和游戏思路";

    public TestApp(ChatModel dashscopeChatModel) {
        //构建会话记忆
        MessageWindowChatMemory chatMemory=MessageWindowChatMemory.builder()
                .maxMessages(20)
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .build();
        chatClient= ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
    public String dochat(String message,String chatId){
        ChatResponse chatResponse = chatClient.prompt().user(message).advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content=chatResponse.getResult().getOutput().getText();
        log.info("content:{}",content);
        return content;
    }
}
