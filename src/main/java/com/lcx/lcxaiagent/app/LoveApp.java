package com.lcx.lcxaiagent.app;


import com.lcx.lcxaiagent.advisor.MyLoggerAdvisor;
import com.lcx.lcxaiagent.advisor.ReReadingAdvisor;
import com.lcx.lcxaiagent.chatmemory.FileBasedChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

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
       /* MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()*/
        /*3.2期 初始化基于文件的对话记忆*/
        String fileDir=System.getProperty("user.dir")+"/tmp/chat-memory";
        ChatMemory chatMemory=new FileBasedChatMemory(fileDir);

        // 构建聊天客户端
        chatClient=ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                        /*自定义日志 Advisor，可按需开启*/
//                        new MyLoggerAdvisor(),
                        /*按需开启 自定义推理增强advisor 但有个弊端 token翻倍 成本太高*/
//                        new ReReadingAdvisor()
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
                /*.advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))*/
                /*如果不传对话 ID（chatId），或者每次传不同的 ID，
                Spring AI 就会认为这是一次全新的对话，
                不会加载之前的聊天记录 → 上下文丢失 → 模型“失忆”。
                而如果始终传同一个 chatId，
                Spring AI 就知道：“哦，这是同一个用户/会话”，
                于是自动把之前的所有问答历史加到当前请求里 → 模型能结合上下文回答。*/
                /*只有传入相同的 chatId，Spring AI 才能把多次请求识别为“同一个对话”，从而自动加载历史消息，实现上下文记忆。
                否则，每次都是“全新对话”，模型无法联系之前的问题。*/
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content=chatResponse.getResult().getOutput().getText();
        log.info("content:{}",content);
        return content;
    }

    record LoveReport(String title, List<String> suggestions){}

    /**
     * AI恋爱报告功能(实战结构化输出)
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结构，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)) // 👈 手动传字符串 key
                .call()
                .entity(LoveReport.class);

        log.info("loveReport:{}", loveReport);
        return loveReport;
    }
    /**
     * 和 RAG 知识库进行对话
     *
     * @param message
     * @param chatId
     * @return
     */
    @Resource
    private VectorStore loveAppVectorStore;

    @Resource
    private Advisor loveAppRagCloudAdvisor;
    public String doChatWithRag(String message, String chatId) {
        // 查询重写
        ChatResponse chatResponse = chatClient
                .prompt()
//                 使用改写后的查询
//                .user(rewrittenMessage)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用 RAG 知识库问答
//                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                // 应用 RAG 检索增强服务（基于云知识库服务）
                .advisors(loveAppRagCloudAdvisor)
                // 应用 RAG 检索增强服务（基于 PgVector 向量存储）
//                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                // 应用自定义的 RAG 检索增强服务（文档查询器 + 上下文增强器）
//                .advisors(
//                        LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(
//                                loveAppVectorStore, "单身"
//                        )
//                )
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}
