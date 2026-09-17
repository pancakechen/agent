package com.api.aiagent.config;


import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagAdvisorConfig {


    @Resource
    private ToolCallback[] allTools;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 ChatMemory chatMemory,
                                 VectorStore vectorStore
                                ) {

        System.out.println("实际注入的 ChatMemory 实现 = " + chatMemory.getClass().getName());
        System.out.println("实际ChatCLinet.Builder"+builder.getClass().getName());

        return builder
                .defaultSystem("""
                    你是一个恋爱大师，请以恋爱大师的口吻回答问题。
                    如果检索到的资料中没有相关内容，直接明确说明知识库中没有相关信息，不要编造""")
                .defaultToolCallbacks(allTools)
                .defaultAdvisors(
                        // 1️⃣ 记忆 advisor：把历史对话注入 prompt（读 memory）
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 2️⃣ 检索 advisor：基于用户问题做向量检索，把结果拼进 prompt
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .topK(4)                       // 召回 4 条
                                        .similarityThreshold(0.5)      // 相似度阈值，低于则丢弃
                                        .build())
                                .build(),
                        // 3️⃣ 日志 advisor：打印最终 prompt，调试 RAG 必备
                        new SimpleLoggerAdvisor())
                .build();
    }
}
