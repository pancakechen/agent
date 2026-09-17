package com.api.aiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Component
public class TerminalTools {

    private final AgentToolSupport support;

    public TerminalTools(AgentToolSupport support) {
        this.support = support;
    }

    @Tool(description = "在受限工作目录中执行一个只读终端命令；仅允许配置白名单中的命令，禁止 shell 管道、重定向、命令串联和命令替换")
    public String execute(@ToolParam(description = "要执行的命令，例如 git status --short") String command) {
        try {
            List<String> arguments = tokenize(command);
            if (arguments.isEmpty()) {
                return "终端执行失败：命令不能为空";
            }
            String executable = arguments.get(0);
            if (!support.properties().getAllowedCommands().contains(executable)) {
                return "终端执行失败：命令不在白名单中：" + executable;
            }
            validateArguments(arguments);

            Path workspace = support.workspace();
            ProcessBuilder processBuilder = new ProcessBuilder(arguments)
                    .directory(workspace.toFile());
            processBuilder.environment().remove("ZHIPU_API_KEY");
            processBuilder.environment().remove("TAVILY_API_KEY");
            processBuilder.environment().remove("OPENAI_API_KEY");

            Process process = processBuilder.start();
            ExecutorService executor = Executors.newFixedThreadPool(2);
            Future<String> stdout = executor.submit(() -> support.readLimited(
                    process.getInputStream(), support.properties().getMaxOutputChars() * 4));
            Future<String> stderr = executor.submit(() -> support.readLimited(
                    process.getErrorStream(), support.properties().getMaxOutputChars() * 4));
            boolean finished = process.waitFor(support.properties().getTerminalTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
            }
            String output = stdout.get(2, TimeUnit.SECONDS);
            String error = stderr.get(2, TimeUnit.SECONDS);
            output = limit(output);
            error = limit(error);
            return "退出码：" + (finished ? process.exitValue() : "超时并已终止")
                    + "\n标准输出：\n" + output
                    + (error.isBlank() ? "" : "\n标准错误：\n" + error);
        } catch (Exception e) {
            return "终端执行失败：" + e.getMessage();
        }
    }

    private List<String> tokenize(String command) {
        if (command == null || command.isBlank()) {
            return List.of();
        }
        if (command.matches(".*[;&|><`$()\\r\\n].*")) {
            throw new IllegalArgumentException("检测到被禁止的 shell 控制符");
        }
        List<String> arguments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        for (char character : command.trim().toCharArray()) {
            if (quote != 0) {
                if (character == quote) {
                    quote = 0;
                } else {
                    current.append(character);
                }
            } else if (character == '\'' || character == '"') {
                quote = character;
            } else if (Character.isWhitespace(character)) {
                if (!current.isEmpty()) {
                    arguments.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(character);
            }
        }
        if (quote != 0) {
            throw new IllegalArgumentException("引号未闭合");
        }
        if (!current.isEmpty()) {
            arguments.add(current.toString());
        }
        return arguments;
    }

    private void validateArguments(List<String> arguments) {
        for (String argument : arguments.subList(1, arguments.size())) {
            if (argument.startsWith("/") || argument.matches(".*(^|/)\\.\\.(/|$).*")) {
                throw new IllegalArgumentException("命令参数不能访问工作目录之外的路径");
            }
        }

        String executable = arguments.get(0);
        if ("find".equals(executable)
                && arguments.stream().anyMatch(argument -> List.of(
                "-delete", "-exec", "-execdir", "-ok", "-okdir", "-fdelete").contains(argument))) {
            throw new IllegalArgumentException("find 的删除和执行参数被禁止");
        }
        if ("git".equals(executable)) {
            if (arguments.size() < 2 || !List.of(
                    "status", "log", "diff", "show", "branch", "rev-parse").contains(arguments.get(1))) {
                throw new IllegalArgumentException("仅允许只读 git 子命令：status、log、diff、show、branch、rev-parse");
            }
            if (arguments.stream().anyMatch(argument -> List.of(
                    "-D", "-d", "-m", "-M", "-c", "-C", "--output", "--exec-path", "--upload-pack")
                    .contains(argument) || argument.startsWith("--output="))) {
                throw new IllegalArgumentException("git 写入或外部执行参数被禁止");
            }
        }
    }

    private String limit(String value) {
        int max = support.properties().getMaxOutputChars();
        return value.length() > max ? value.substring(0, max) + "\n[输出已截断]" : value;
    }
}
