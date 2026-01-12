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
/**
 * 恋爱心理咨询AI应用类
 * 提供基于AI的恋爱心理咨询对话功能，支持多轮对话记忆
 */
@Component
@Slf4j
public class LoveApp {
    /**
     * AI聊天客户端实例，用于与AI模型交互
     */
    private final ChatClient chatClient;
    /**
     * 系统提示词，定义AI角色和行为准则
     * 定义为恋爱心理专家角色，根据不同用户状态（单身/恋爱/已婚）提供针对性咨询
     */
    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";

    /**
     * 初始化
     * 构造函数，初始化聊天客户端
     *
     * @param dashscopeChatModel 通义千问聊天模型实例
     */
    public LoveApp(ChatModel dashscopeChatModel){
        //初始化基于内存的对话记忆
        //创建消息窗口聊天记忆，限制最大消息数为20条
        //创建一个基于滑动窗口机制的聊天记忆（Chat Memory）对象
        // 用于在对话系统中保留最近若干条消息的历史记录，以便大模型在生成回复时能够参考上下文。

        //MessageWindowChatMemory:Spring AI 提供的一种 聊天记忆实现类 它采用“滑动窗口”策略
        //只保留最近 N 条用户与 AI 的交互消息（包括用户提问和 AI 回答）
        //当消息数量超过设定上限时，自动丢弃最早的消息，确保上下文长度可控，避免超出模型 token 限制或性能下降。
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                //chatMemoryRepository:指定存储聊天记录的仓库

                //InMemoryChatMemoryRepository 表示将聊天历史保存在内存中（JVM 内存）
                //优点：简单、快速，适合单机、开发测试或短期会话。
                //缺点：不持久化，服务重启后历史丢失；不支持多实例共享（如集群部署时各节点无法共享同一用户的对话历史）。

                //若需持久化或分布式支持，可替换为 Redis、数据库等实现（如自定义 ChatMemoryRepository）
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                //设置最多保留 20 条消息（注意：是“消息条数”，不是 token 数）。
                //每次新对话加入后，如果总消息数 > 20，就从最旧的一条开始删除，直到 ≤ 20。
                //这 20 条通常包含交替的用户输入（User Message）和 AI 回复（AI Message）
                .maxMessages(20)
                .build();
        // 构建聊天客户端
        chatClient=ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }
    /*AI基础对话(支持多轮对话记忆)*/
    /**
     * 执行 AI 基础对话，支持基于会话 ID（chatId）的多轮对话上下文记忆。
     *
     * @param message 用户当前输入的消息内容
     * @param chatId  唯一会话标识符，用于关联同一用户的连续对话历史
     * @return        AI 模型生成的回复文本
     */
    public String doChat(String message,String chatId){
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content=chatResponse.getResult().getOutput().getText();
        log.info("content:{}",content);
        return content;
    }

}
