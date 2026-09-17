package com.api.aiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.InetAddress;

@Component
public class WebFetchTools {

    private final HttpClient httpClient;
    private final AgentToolSupport support;

    public WebFetchTools(HttpClient httpClient, AgentToolSupport support) {
        this.httpClient = httpClient;
        this.support = support;
    }

    @Tool(description = "抓取一个公开网页并提取可读文本；只允许 http 或 https 地址，并限制返回内容大小")
    public String fetch(@ToolParam(description = "要抓取的网页 URL") String url) {
        try {
            URI uri = URI.create(url);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                return "网页抓取失败：只允许不带用户凭据的 http/https URL";
            }
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                        || address.isLinkLocalAddress() || address.isSiteLocalAddress()) {
                    return "网页抓取失败：不允许访问本机或内网地址";
                }
            }

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(support.properties().getHttpTimeout())
                    .header("User-Agent", "ai-agent-web-fetch/1.0")
                    .header("Accept", "text/html,text/plain;q=0.9,*/*;q=0.5")
                    .GET()
                    .build();
            HttpResponse<java.io.InputStream> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            String body = support.readLimited(response.body(), support.properties().getMaxResponseBytes());
            if (response.statusCode() / 100 != 2) {
                return "网页抓取失败（HTTP " + response.statusCode() + "）";
            }
            String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase();
            String text = contentType.contains("html") ? extractText(body) : body;
            text = text.trim();
            int maxLength = support.properties().getMaxWebContentChars();
            return text.length() > maxLength ? text.substring(0, maxLength) + "\n[内容已截断]" : text;
        } catch (Exception e) {
            return "网页抓取失败：" + e.getMessage();
        }
    }

    private String extractText(String html) throws Exception {
        StringBuilder text = new StringBuilder();
        new ParserDelegator().parse(new StringReader(html), new HTMLEditorKit.ParserCallback() {
            private int ignoredDepth;

            @Override
            public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
                if (isIgnored(tag)) {
                    ignoredDepth++;
                } else if (isBlock(tag)) {
                    text.append('\n');
                }
            }

            @Override
            public void handleEndTag(HTML.Tag tag, int position) {
                if (isIgnored(tag) && ignoredDepth > 0) {
                    ignoredDepth--;
                } else if (isBlock(tag)) {
                    text.append('\n');
                }
            }

            @Override
            public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
                if (tag == HTML.Tag.BR) {
                    text.append('\n');
                }
            }

            @Override
            public void handleText(char[] data, int position) {
                if (ignoredDepth == 0) {
                    text.append(data).append(' ');
                }
            }

            private boolean isIgnored(HTML.Tag tag) {
                return tag == HTML.Tag.SCRIPT || tag == HTML.Tag.STYLE || tag == HTML.Tag.HEAD
                        || "noscript".equalsIgnoreCase(tag.toString());
            }

            private boolean isBlock(HTML.Tag tag) {
                return tag == HTML.Tag.P || tag == HTML.Tag.DIV || tag == HTML.Tag.LI
                        || tag == HTML.Tag.H1 || tag == HTML.Tag.H2 || tag == HTML.Tag.H3
                        || tag == HTML.Tag.H4 || tag == HTML.Tag.H5 || tag == HTML.Tag.H6;
            }
        }, true);
        return text.toString().replaceAll("[ \\t]+", " ")
                .replaceAll("\\n[ \\n]+", "\n")
                .replaceAll("\\n{3,}", "\n\n");
    }
}
