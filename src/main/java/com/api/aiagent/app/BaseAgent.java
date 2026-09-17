package com.api.aiagent.app;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

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

    // Spring AI 记忆功能
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

        // 从 ChatMemory 加载历史消息
        loadHistoryFromChatMemory();

        agentState = AgentState.RUNNING;
        UserMessage userMessage = new UserMessage(userPrompt);
        messagesList.add(userMessage);

        try {
            for (int i = 0; i < maxStep && agentState == AgentState.RUNNING; i++){
                currentStep = i + 1;
                log.info("当前正在执行第: " + currentStep + "步");
                step();
            }

            if (agentState == AgentState.FINISHED) {
                // 保存用户问题和最终回答到 ChatMemory
                saveConversationToChatMemory(userMessage, finalAnswer);
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
     * 从 ChatMemory 加载历史消息（适用于 Spring AI 1.1.8）
     */
    private void loadHistoryFromChatMemory() {
        if (chatMemory != null && !StringUtils.isEmpty(conversationId)) {
            try {
                List<Message> historyMessages = chatMemory.get(conversationId);
                if (historyMessages != null && !historyMessages.isEmpty()) {
                    messagesList.addAll(historyMessages);
                    log.info("从 ChatMemory 加载了 " + historyMessages.size() + " 条历史消息");
                }
            } catch (Exception e) {
                log.warn("加载历史消息失败：" + e.getMessage());
            }
        }
    }

    /**
     * 保存对话到 ChatMemory - 只保存用户问题和最终回答（适用于 Spring AI 1.1.8）
     */
    private void saveConversationToChatMemory(UserMessage userMessage, String finalAnswer) {
        if (chatMemory != null && !StringUtils.isEmpty(conversationId) && !StringUtils.isEmpty(finalAnswer)) {
            try {
                List<Message> toSave = new ArrayList<>();
                toSave.add(userMessage);
                toSave.add(new AssistantMessage(finalAnswer));
                chatMemory.add(conversationId, toSave);
                log.info("已保存对话到 ChatMemory");
            } catch (Exception e) {
                log.warn("保存对话到 ChatMemory 失败：" + e.getMessage());
            }
        }
    }


}
