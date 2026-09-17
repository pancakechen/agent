package com.api.aiagent.tools;

import com.api.aiagent.config.AgentToolProperties;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class AgentToolSupport {

    private final AgentToolProperties properties;

    public AgentToolSupport(AgentToolProperties properties) {
        this.properties = properties;
    }

    public Path workspace() throws IOException {
        Path configured = Path.of(properties.getWorkspaceDirectory());
        Path workspace = configured.isAbsolute()
                ? configured.normalize()
                : Path.of(System.getProperty("user.dir")).resolve(configured).normalize();
        Files.createDirectories(workspace);
        return workspace.toRealPath();
    }

    public Path resolveWorkspacePath(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("路径不能为空");
        }
        Path input = Path.of(relativePath);
        if (input.isAbsolute()) {
            throw new IllegalArgumentException("只允许使用工作目录下的相对路径");
        }

        Path workspace = workspace();
        Path candidate = workspace.resolve(input).normalize();
        if (!candidate.startsWith(workspace)) {
            throw new IllegalArgumentException("路径不能离开工作目录");
        }

        if (Files.exists(candidate)) {
            if (!Files.isSameFile(workspace, candidate) && !candidate.toRealPath().startsWith(workspace)) {
                throw new IllegalArgumentException("不允许访问工作目录外的符号链接");
            }
        } else if (candidate.getParent() != null && Files.exists(candidate.getParent())
                && !candidate.getParent().toRealPath().startsWith(workspace)) {
            throw new IllegalArgumentException("不允许访问工作目录外的符号链接");
        }
        return candidate;
    }

    public String readLimited(InputStream inputStream, int maxBytes) throws IOException {
        try (InputStream input = inputStream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int length;
            while ((length = input.read(buffer)) != -1) {
                total += length;
                if (total > maxBytes) {
                    throw new IOException("响应内容超过大小限制");
                }
                output.write(buffer, 0, length);
            }
            return output.toString(java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    public AgentToolProperties properties() {
        return properties;
    }
}
