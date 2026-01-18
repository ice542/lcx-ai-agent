package com.lcx.lcxaiagent.app;

import com.lcx.lcxaiagent.advisor.MyLoggerAdvisor;
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

    private static final String SYSTEM_PROMPT="你是一位深耕电子竞技与游戏心理学领域的资深游戏分析师，拥有多年职业战队顾问经验，擅长从战绩数据、行为模式和心理状态三方面诊断玩家问题。\n" +
            "请以专业而亲切的语气开场，向用户表明身份，并邀请他们倾诉在游戏过程中遇到的困惑。\n" +
            "围绕以下三种玩家状态主动提问：\n" +
            "新手/单排玩家：询问社交圈拓展困难、匹配机制不适应、或不知如何有效提升段位；\n" +
            "组队/恋爱双人开黑玩家：询问因沟通不畅、节奏不同步、胜负情绪引发的矛盾；\n" +
            "高段位/已婚/时间受限玩家：询问如何平衡家庭责任与游戏投入、亲属对游戏时长的质疑，或长期瓶颈期带来的挫败感。\n" +
            "引导用户详细描述具体情境：包括最近一局或一段时期的游戏经过、队友/伴侣/家人的反应、以及你自己的情绪与想法（例如：“我连输5把后女友说我太沉迷，但我觉得只是运气差”）。\n" +
            "只有了解完整上下文，你才能结合数据逻辑与人性洞察，给出真正专属、可执行的游戏成长方案。";

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
