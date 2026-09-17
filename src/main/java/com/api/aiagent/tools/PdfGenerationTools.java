package com.api.aiagent.tools;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfGenerationTools {

    private static final float MARGIN = 50;
    private static final float FONT_SIZE = 11;
    private static final float LINE_HEIGHT = 16;

    private final AgentToolSupport support;

    public PdfGenerationTools(AgentToolSupport support) {
        this.support = support;
    }

    @Tool(name="PdfGenerationTools",description = "把文本生成 PDF 文件并保存到工作目录；支持中文时必须配置 app.agent-tools.pdf-font-path 指向 TTF 字体")
    public String generate(
            @ToolParam(description = "工作目录下的 PDF 相对路径，例如 output/report.pdf") String relativePath,
            @ToolParam(description = "要写入 PDF 的文本内容") String content) {
        try {
            Path output = support.resolveWorkspacePath(relativePath);
            if (!relativePath.toLowerCase().endsWith(".pdf")) {
                return "PDF 生成失败：输出文件必须以 .pdf 结尾";
            }
            if (output.getParent() != null) {
                Files.createDirectories(output.getParent());
            }
            try (PDDocument document = new PDDocument()) {
                PDFont font = loadFont(document, content == null ? "" : content);
                List<String> lines = wrap(content == null ? "" : content, font);
                PDPageContentStream stream = null;
                float y = 0;
                try {
                    for (String line : lines) {
                        if (stream == null || y < MARGIN + LINE_HEIGHT) {
                            if (stream != null) {
                                stream.endText();
                                stream.close();
                            }
                            PDPage page = new PDPage(PDRectangle.A4);
                            document.addPage(page);
                            stream = new PDPageContentStream(document, page);
                            stream.beginText();
                            stream.setFont(font, FONT_SIZE);
                            y = page.getMediaBox().getHeight() - MARGIN;
                            stream.newLineAtOffset(MARGIN, y);
                        }
                        stream.showText(line);
                        stream.newLineAtOffset(0, -LINE_HEIGHT);
                        y -= LINE_HEIGHT;
                    }
                } finally {
                    if (stream != null) {
                        stream.endText();
                        stream.close();
                    }
                }
                document.save(output.toFile());
            }
            return "PDF 生成成功：" + relativePath;
        } catch (Exception e) {
            return "PDF 生成失败：" + e.getMessage();
        }
    }

    private PDFont loadFont(PDDocument document, String content) throws Exception {
        String fontPath = support.properties().getPdfFontPath();
        if (fontPath != null && !fontPath.isBlank()) {
            Path font = Path.of(fontPath).toAbsolutePath().normalize();
            if (!Files.isRegularFile(font)) {
                throw new IllegalArgumentException("配置的 PDF 字体文件不存在");
            }
            return PDType0Font.load(document, font.toFile());
        }
        if (content.codePoints().anyMatch(codePoint -> codePoint > 255)) {
            throw new IllegalArgumentException("文本包含非 WinAnsi 字符，请配置 app.agent-tools.pdf-font-path");
        }
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    private List<String> wrap(String content, PDFont font) throws Exception {
        float maxWidth = PDRectangle.A4.getWidth() - MARGIN * 2;
        List<String> result = new ArrayList<>();
        for (String paragraph : content.replace("\r\n", "\n").split("\n", -1)) {
            if (paragraph.isEmpty()) {
                result.add("");
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (int offset = 0; offset < paragraph.length(); ) {
                int codePointLength = Character.charCount(paragraph.codePointAt(offset));
                String next = paragraph.substring(offset, offset + codePointLength);
                String candidate = line + next;
                if (!line.isEmpty() && font.getStringWidth(candidate) / 1000 * FONT_SIZE > maxWidth) {
                    result.add(line.toString());
                    line.setLength(0);
                } else {
                    line.append(next);
                    offset += codePointLength;
                }
            }
            if (!line.isEmpty()) {
                result.add(line.toString());
            }
        }
        return result.isEmpty() ? List.of("") : result;
    }
}
