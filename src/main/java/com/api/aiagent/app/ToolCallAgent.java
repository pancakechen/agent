package com.api.aiagent.app;


import cn.hutool.core.collection.CollUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工具调用类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Log4j2
public class ToolCallAgent extends ReActAgent {

    // 可用工具
    private final ToolCallback[] availableTools;

    //    保存工具调用的响应信息
    private ChatResponse toolChatResponse;

    //    工具调用管理者
    private final ToolCallingManager toolCallingManager;

    //    禁用内置的工具调用机制，自己维护上下文
    private final ToolCallingChatOptions toolCallingChatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
//         禁用内置的工具调用机制，自己维护上下文
/*
        this.toolCallingChatOptions = ToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(false)
                .build();
*/
        // 手动执行工具时，回调必须随 Prompt 传入。
        this.toolCallingChatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(availableTools)
                .internalToolExecutionEnabled(false)
                .build();

    }


/*
    @Override
    public boolean think() {
//        引入上一轮对话的结果
        if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessagesList().add(userMessage);
        }
//          创建新的提示词
        List<Message> messagesList = getMessagesList();
        Prompt prompt = new Prompt(messagesList, toolCallingChatOptions);

        try {
//            与ai对话 获取对话结果
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .tools(availableTools)
                    .call()
                    .chatResponse();

//       记录响应结果 用于act
            this.toolChatResponse = chatResponse;
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            String result = assistantMessage.getText();
            List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
            log.info(getName() + "的思考：" + result);
            log.info(getName() + "选择了" + toolCalls.size() + "个工具来使用");
            String toolCallInfo = toolCalls.stream().map(
                    toolCall -> String.format("工具名称：%s,参数：%s", toolCall.name(), toolCall.arguments())
            ).collect(Collectors.joining("\n"));

            log.info(toolCallInfo);

//            如果ai没有调用工具 返回false
            if (toolCallInfo.isEmpty()) {
                getMessagesList().add(assistantMessage);
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            log.error(getName() + "的思考遇到了问题：" + e.getMessage());
//            将错误信息添加到message列表里 方便ai下一步处理问题
            getMessagesList().add(
                    new AssistantMessage("处理遇到的错误：" + e.getMessage())
            );
            return false;
        }
    }
*/

    @Override
    public boolean think() {
        Prompt prompt = new Prompt(getMessagesList(), toolCallingChatOptions);

        try {
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .call()
                    .chatResponse();

            this.toolChatResponse = chatResponse;
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            log.info(getName() + "的思考：" + assistantMessage.getText());

            if (!chatResponse.hasToolCalls()) {
                getMessagesList().add(assistantMessage);
                setFinalAnswer(assistantMessage.getText());
                setAgentState(AgentState.FINISHED);
                return false;
            }

            List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
            String toolCallInfo = toolCalls.stream().map(
                    toolCall -> String.format("工具名称：%s,参数：%s", toolCall.name(), toolCall.arguments())
            ).collect(Collectors.joining("\n"));
            log.info(getName() + "选择了" + toolCalls.size() + "个工具来使用");
            log.info(toolCallInfo);
            return true;
        } catch (Exception e) {
            setAgentState(AgentState.ERROR);
            setFinalAnswer("智能体思考失败：" + e.getMessage());
            log.error(getName() + "的思考遇到了问题：" + e.getMessage());
            return false;
        }
    }

/*
    @Override
    public String act() {

        if(!toolChatResponse.hasToolCalls()){
            return "没有工具调用";
        }
//        工具调用
        Prompt prompt = new Prompt(getMessagesList(), toolCallingChatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolChatResponse);
//        记录消息上下文 conversationHistory里包含了助手消息和工具调用返回结果
        setMessagesList(toolExecutionResult.conversationHistory());
//        当前工具调用结果
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        String result = toolResponseMessage.getResponses().stream().map(
                response -> "工具" + response.name() +"任务完成！ 结果："+response.responseData()
        ).collect(Collectors.joining("\n"));

        log.info(result);

        return result;
    }
*/

    @Override
    public String act() {

        if(toolChatResponse == null || !toolChatResponse.hasToolCalls()){
            setAgentState(AgentState.ERROR);
            setFinalAnswer("工具执行失败：没有可执行的工具调用");
            return getFinalAnswer();
        }

        Prompt prompt = new Prompt(getMessagesList(), toolCallingChatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolChatResponse);
        // conversationHistory 包含本轮 AssistantToolCall 和 ToolResponse，下一轮会将其作为 Prompt 上下文。
        setMessagesList(toolExecutionResult.conversationHistory());

        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        String result = toolResponseMessage.getResponses().stream().map(
                response -> "工具" + response.name() +"任务完成！ 结果："+response.responseData()
        ).collect(Collectors.joining("\n"));

        log.info(result);

        return result;
    }
}
