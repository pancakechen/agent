package com.api.aiagent.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/smoke")
public class SmokeChatController {

    private final ChatClient chatClient;

    public SmokeChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message,
                       @RequestParam(defaultValue = "demo-conversation") String conversationId) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(
                        org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID,
                        conversationId))
                .call()
                .content();
    }

    @GetMapping("/chat/stream")
    public Flux<String> chatStream(@RequestParam String message,
                                   @RequestParam(defaultValue = "demo-conversation") String conversationId) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(
                        org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID,
                        conversationId))
                .stream()
                .content();
    }
}
