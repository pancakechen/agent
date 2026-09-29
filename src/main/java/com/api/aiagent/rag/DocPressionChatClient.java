package com.api.aiagent.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class DocPressionChatClient {

    //  构造器引入 具体写法参照官方文档
    private final ChatClient chatClient;


    public DocPressionChatClient(@Qualifier("zhiPuAiChatModel") ChatModel chatModel,
                        ToolCallback[] allTools,
                        ToolCallbackProvider toolCallbackProvider) {

        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                    你是一个文档压缩专家，你会把用户给你的所有的文档精简总结，但是又不会影响用户想表达的意图""")
                .defaultToolCallbacks(allTools)
                .build();
    }

    public String ChatClientResponse(String userInput){
        return this.chatClient
                .prompt()
                .user(userInput)
                .call()
                .content();
    }
}
