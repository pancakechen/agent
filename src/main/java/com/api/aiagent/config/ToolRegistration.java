package com.api.aiagent.config;


import com.api.aiagent.tools.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.Arrays;
import java.util.stream.Stream;

@Configuration
public class ToolRegistration {

    private final AgentToolSupport support;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AgentToolProperties properties;

    public ToolRegistration(AgentToolSupport support, HttpClient httpClient, ObjectMapper objectMapper, AgentToolProperties properties) {
        this.support = support;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Bean
    public ToolCallback[]  allTools(ToolCallbackProvider toolCallbackProvider) throws IOException {


        PdfGenerationTools pdfGenerationTools = new PdfGenerationTools(support);

        TerminalTools terminalTools = new TerminalTools(support);

        WebFetchTools webFetchTools = new WebFetchTools(httpClient, support);

        WebSearchTools webSearchTools = new WebSearchTools(httpClient, objectMapper, support, properties);

        ResourceDownloadTools resourceDownloadTools = new ResourceDownloadTools();

        WebScrapingTools webScrapingTools = new WebScrapingTools();

        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();

        FileOperationTool fileOperationTool = new FileOperationTool();


        ToolCallback[] myTools = ToolCallbacks.from(
                resourceDownloadTools,
                pdfGenerationTool,
                terminalTools,
                fileOperationTool
        );

        // 合并：自己的工具 + 所有 MCP server 的工具
        ToolCallback[] merged = Stream.concat(
                        Arrays.stream(myTools),
                        Arrays.stream(toolCallbackProvider.getToolCallbacks()))
                .toArray(ToolCallback[]::new);

        System.out.println("已注册工具共 " + merged.length + " 个：");
        Arrays.stream(merged).forEach(t ->
                System.out.println("  - " + t.getToolDefinition().name()));

        return merged;

    }

}
