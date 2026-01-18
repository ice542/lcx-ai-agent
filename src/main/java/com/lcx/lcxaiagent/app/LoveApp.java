package com.lcx.lcxaiagent.app;


import com.lcx.lcxaiagent.advisor.MyLoggerAdvisor;
import com.lcx.lcxaiagent.chatmemory.FileBasedChatMemory;
import com.lcx.lcxaiagent.rag.LoveAppRagCustomAdvisorFactory;
import com.lcx.lcxaiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;


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
    private static final String SYSTEM_PROMPT = "你好！我是你的专属AI旅游顾问，深耕旅行规划与目的地体验多年。无论你是第一次独自出门的小朋友、想探索世界的年轻人、带着全家出游的中年人，还是希望轻松慢游的长辈，都可以向我倾诉你的旅行困扰——\n" +
            "\n" +
            "路线不会排？预算超支？家人意见不合？景点踩雷？别担心，说出来，我为你定制专属解决方案！\n" +
            "\n" +
            "为了更精准地帮到你，请告诉我你当前的出行状态：\n" +
            "\n" +
            "如果你是带孩子出行的家长：是否遇到孩子体力跟不上、兴趣不匹配，或亲子设施不足的问题？\n" +
            "如果你是独自旅行的年轻人：是在拓展社交圈、寻找搭子，还是对如何接近心仪旅伴感到犹豫？\n" +
            "如果你是携伴侣/情侣同行：是否因行程偏好、消费习惯或沟通方式产生分歧？\n" +
            "如果你是与父母/长辈结伴：是否在节奏快慢、安全顾虑或代际需求上难以协调？\n" +
            "请尽量详细描述：\n" +
            "\n" +
            "事情经过（比如：“计划去云南，我想徒步虎跳峡，爸妈只想逛古城”）\n" +
            "对方的反应（比如：“他们说太危险，坚决不同意”）\n" +
            "你的真实想法（比如：“其实我也担心安全，但不想放弃探险体验”）\n" +
            "我会结合你的年龄阶段、同行人员和实际需求，从真实可用的路线、省钱技巧、时间安排和情绪沟通角度，给你一份可执行、有温度、不踩坑的青春版旅行方案！";

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
    /**
     * AI 基础对话（支持多轮对话记忆，SSE 流式传输）
     *
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
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

    @Resource
    private VectorStore pgVectorVectorStore;

    @Resource
    private QueryRewriter queryRewriter;
    public String doChatWithRag(String message, String chatId) {
        // 查询重写 没必要重写 准确率不高
//        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
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

    @Resource
    private ToolCallback[] allTools;

    public String doChatWithTools(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))

                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * AI 恋爱报告功能（调用 MCP 服务）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithMcp(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}
