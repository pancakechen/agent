package com.api.aiagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.agent-tools")
public class AgentToolProperties {

    private String workspaceDirectory = "agent-workspace";
    private String searchApiUrl = "https://api.tavily.com/search";
    private String searchApiKey;
    private Duration httpTimeout = Duration.ofSeconds(10);
    private Duration terminalTimeout = Duration.ofSeconds(5);
    private int maxOutputChars = 8_000;
    private int maxFileContentChars = 20_000;
    private int maxWebContentChars = 20_000;
    private int maxResponseBytes = 2 * 1024 * 1024;
    private String pdfFontPath;
    private List<String> allowedCommands = new ArrayList<>(List.of(
            "pwd", "ls", "find", "rg", "cat", "head", "tail", "wc", "git"));
}
