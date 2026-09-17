package com.api.aiagent.tools;

import com.api.aiagent.config.AgentToolProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class WebSearchTools {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AgentToolSupport support;
    private final AgentToolProperties properties;

    public WebSearchTools(HttpClient httpClient,
                          ObjectMapper objectMapper,
                          AgentToolSupport support,
                          AgentToolProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.support = support;
        this.properties = properties;
    }

    @Tool(description = "联网搜索公开信息，返回结果标题、链接和摘要；需要配置 app.agent-tools.search-api-key")
    public String search(@ToolParam(description = "要搜索的关键词或问题") String query) {
        if (query == null || query.isBlank()) {
            return "搜索失败：查询内容不能为空";
        }
        if (properties.getSearchApiKey() == null || properties.getSearchApiKey().isBlank()) {
            return "搜索失败：尚未配置联网搜索 API Key";
        }
        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("api_key", properties.getSearchApiKey());
            requestBody.put("query", query);
            requestBody.put("search_depth", "basic");
            requestBody.put("max_results", 5);
            requestBody.put("include_answer", false);

            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getSearchApiUrl()))
                    .timeout(properties.getHttpTimeout())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(requestBody), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<java.io.InputStream> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            String body = support.readLimited(response.body(), properties.getMaxResponseBytes());
            if (response.statusCode() / 100 != 2) {
                return "搜索服务返回错误（HTTP " + response.statusCode() + "）：" + body;
            }

            JsonNode results = objectMapper.readTree(body).path("results");
            StringBuilder output = new StringBuilder();
            for (JsonNode result : results) {
                output.append("标题：").append(result.path("title").asText()).append('\n')
                        .append("链接：").append(result.path("url").asText()).append('\n')
                        .append("摘要：").append(result.path("content").asText()).append("\n\n");
            }
            return output.isEmpty() ? "没有找到相关搜索结果" : output.toString().trim();
        } catch (Exception e) {
            return "搜索失败：" + e.getMessage();
        }
    }
}
