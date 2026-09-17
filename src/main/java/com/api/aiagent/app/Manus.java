package com.api.aiagent.app;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/**
 * 最终可使用的实例
 */
@Component
public class Manus extends ToolCallAgent{

    private final ToolCallback[] availableTools;
    private final ChatModel chatModel;
    private final ChatMemory chatMemory;
    private final VectorStore vectorStore;

    private String id = "demo-conversation";

    public Manus(ToolCallback[] availableTools, ChatModel chatModel, ChatMemory chatMemory, VectorStore vectorStore) {
        super(availableTools);
        this.setName("Manus");
        this.availableTools = availableTools;
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
        this.vectorStore = vectorStore;

/*
        String systemPrompt = "You are OpenManus, an all-capable AI assistant,  +\n" +
                "                aimed at solving any task presented by the user.  +\n" +
                "                You have various tools at your disposal  +\n" +
                "                that you can call upon to efficiently complete complex requests.";
*/
        String systemPrompt = """
                You are OpenManus, an all-capable AI assistant aimed at solving user tasks.
                Use the available tools when needed. After each tool result, decide whether another tool call is needed.
                When the task is complete, respond directly with the final answer without calling another tool.
                """;
        this.setSystemPrompt(systemPrompt);

/*
        String nextStepPrompt = "Based on user needs, " +
                "proactively select the most appropriate tool " +
                "or combination of tools. " +
                "For complex tasks, " +
                "you can break down the problem and use different tools step by step to solve it. " +
                "After using each tool, clearly explain the execution results and suggest the next steps." +
        "if you want to stop in the interaction at any point,use the `terminate` tool call.";
        this.setNextStepPrompt(nextStepPrompt);
*/
        this.setNextStepPrompt(null);
        this.setMaxStep(20);

        // 默认不启用记忆和知识库的 ChatClient
        ChatClient chatClient = ChatClient.builder(chatModel)
                .build();
        this.setChatClient(chatClient);

    }

    /**
     * 根据 userId 决定是否启用记忆和知识库功能（适用于 Spring AI 1.1.8）
     *
     * @param userInput 用户输入
     * @param userId 用户 ID（可选），为空则不启用记忆和知识库
     * @return Agent 执行结果
     */
    public String run(String userInput, String userId) {
        // 如果传入了 userId，启用记忆和知识库功能
        if (!StringUtils.isEmpty(userId)) {
            // 生成 conversationId
            String conversationId = userId;
            // 构建带记忆和向量检索的 ChatClient
            ChatClient chatClient = ChatClient.builder(chatModel)
                    .defaultAdvisors(
                            MessageChatMemoryAdvisor.builder(chatMemory)
                                    .build(),
                            QuestionAnswerAdvisor.builder(vectorStore)
                                    .searchRequest(SearchRequest.builder()
                                            .topK(4)
                                            .similarityThreshold(0.5)
                                            .build())
                                    .build()
                    )
                    .build();
            this.setChatClient(chatClient);
            this.setChatMemory(chatMemory);
            this.setConversationId(conversationId);
        } else {
            // 没有 userId，使用原有的不带记忆和知识库的 ChatClient
            ChatClient chatClient = ChatClient.builder(chatModel).build();
            this.setChatClient(chatClient);
        }

        // 调用父类的 run 方法
        return super.run(userInput);
    }
}
