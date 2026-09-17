package com.api.aiagent.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

@Component
public class MyChatClient {

//  构造器引入 具体写法参照官方文档
    private final ChatClient chatClient;

    private final ToolCallback[] allTools;

    public MyChatClient(ChatClient.Builder chatClientBuilder,
                        ChatMemory chatMemory,
                        VectorStore vectorStore,
                        ToolCallback[] allTools,
                        ToolCallbackProvider toolCallbackProvider) {
        this.allTools = allTools;

        this.chatClient = chatClientBuilder
                .defaultSystem("""
                    你是一个恋爱大师，请以恋爱大师的口吻回答问题。
                    如果检索到的资料中没有相关内容，直接明确说明知识库中没有相关信息，不要编造""")
                .defaultAdvisors(
                        MessageChatMemoryAdvisor
                                .builder(chatMemory)
                                .build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .topK(4)
                                        .similarityThreshold(0.5)
                                        .build())
                                .build()
                )
                .build();
    }

    public String ChatClientResponse(String userInput,String conversationId){
        return this.chatClient
                .prompt()
                .user(userInput)
                .tools((Object) allTools)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}
