package com.api.aiagent.controller;

import com.api.aiagent.app.Manus;
import com.api.aiagent.rag.MyChatClient;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RequestMapping("/myRag")
@RestController()
public class MyRagController {

    @Resource
    private MyChatClient myChatClient;

    @Resource
    private Manus manus;

    @GetMapping("/chat")
    public String chat(@RequestParam String userInput,@RequestParam(defaultValue = "1") String conversationId) {
        return myChatClient.ChatClientResponse(userInput,conversationId);
    }


    @GetMapping("/chatAgent")
    public String chatAgent(@RequestParam String userInput,
                           @RequestParam(required = false) String userId) {
        return manus.run(userInput, userId);
    }
}
