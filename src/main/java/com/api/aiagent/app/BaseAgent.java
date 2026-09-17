package com.api.aiagent.app;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能体基类，定义基本信息和执行流程
 */
@Log4j2
@Data
public abstract class BaseAgent {

    // 核心属性
    private String name;

    // 提示词
    private String systemPrompt;
    private String nextStepPrompt;

    // 状态
    private AgentState agentState = AgentState.IDLE;

    // 最大执行步骤 防止token过度消耗
    private int maxStep = 10;
    // 当前是哪一步
    private int currentStep = 0;


    // LLM
    private ChatClient chatClient;

    // 上下文记忆
    private List<Message> messagesList = new ArrayList<>();

    // 当前任务的最终回答
    private String finalAnswer;

    // Spring AI 记忆功能（适用于 Spring AI 1.1.8）
    private ChatMemory chatMemory;
    private String conversationId;


/*
    public String run(String userPrompt){

        if (StringUtils.isEmpty(userPrompt)){
            throw new IllegalArgumentException("userPrompt can not be empty");
        }

        if (agentState != AgentState.IDLE){
            throw new RuntimeException("agentState is not idle");
        }
        //更改状态
        agentState = AgentState.RUNNING;
        // 上下文记忆加载
        messagesList.add(new UserMessage(userPrompt));
        // 记录结果列表
        List<String> results = new ArrayList<>();

        try {
            for (int i = 0; i <= maxStep && agentState != AgentState.FINISHED; i++){

                int stepNumber = i;
                currentStep = stepNumber;
                log.info("当前正在执行第: " + stepNumber/maxStep + "步");

                // 单步执行
                String stepResult  = step();
                String result = "step"+stepNumber+":"+stepResult;
                results.add(result);
            }

            // 检查是否超出步骤限制
            if(currentStep >= maxStep){
                agentState = AgentState.FINISHED;

                results.add("已达到最大执行步骤"+maxStep);
            }

            return String.join("\n", results);
        }catch (Exception e){
            agentState = AgentState.ERROR;
            log.error("运行错误："+e.getMessage());
            return "执行错误"+ e.getMessage();
        }finally {
            // 清理资源
            cleanup();
        }
    }
*/

    public synchronized String run(String userPrompt){

        if (StringUtils.isEmpty(userPrompt)){
            throw new IllegalArgumentException("userPrompt can not be empty");
        }

        resetTask();
        agentState = AgentState.RUNNING;
        messagesList.add(new UserMessage(userPrompt));

        try {
            for (int i = 0; i < maxStep && agentState == AgentState.RUNNING; i++){
                currentStep = i + 1;
                log.info("当前正在执行第: " + currentStep + "步");
                step();
            }

            if (agentState == AgentState.FINISHED) {
                return finalAnswer;
            }

            if (agentState == AgentState.RUNNING) {
                agentState = AgentState.ERROR;
                return "执行错误：已达到最大执行步骤" + maxStep;
            }

            return finalAnswer == null ? "执行错误：智能体执行失败" : finalAnswer;
        }catch (Exception e){
            agentState = AgentState.ERROR;
            log.error("运行错误：" + e.getMessage());
            return "执行错误：" + e.getMessage();
        }finally {
            cleanup();
        }
    }

    private void resetTask(){
        agentState = AgentState.IDLE;
        currentStep = 0;
        finalAnswer = null;
        messagesList = new ArrayList<>();
    }

    // 执行单个步骤
    public abstract String step();


    /**
     * 资源清理
     */
    protected void cleanup(){
        //子方法可以重写此方法来清理资源
    }

    /**
     * 启用对话记忆功能（适用于 Spring AI 1.1.8）
     * 使用 MessageChatMemoryAdvisor 自动管理消息的保存和加载
     *
     * @param chatModel ChatModel 实例
     * @param chatMemory ChatMemory 实例
     * @param conversationId 会话 ID，用于区分不同对话
     */
    public void enableChatMemory(ChatModel chatModel, ChatMemory chatMemory, String conversationId) {
        if (chatModel == null || chatMemory == null || StringUtils.isEmpty(conversationId)) {
            throw new IllegalArgumentException("chatModel, chatMemory and conversationId must not be null");
        }

        this.chatMemory = chatMemory;
        this.conversationId = conversationId;

        // 构建带 MessageChatMemoryAdvisor 的 ChatClient（适用于 Spring AI 1.1.8）
        // conversationId 在调用时通过 advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId)) 传入
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();

        log.info("已为 Agent [" + name + "] 启用对话记忆功能，conversationId: " + conversationId);
    }


}
