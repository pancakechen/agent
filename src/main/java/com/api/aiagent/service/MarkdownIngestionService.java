package com.api.aiagent.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MarkdownIngestionService {

    private final VectorStore vectorStore;
    private final JdbcTemplate vectorJdbcTemplate;

    public MarkdownIngestionService(VectorStore vectorStore,JdbcTemplate vectorJdbcTemplate) {
        this.vectorStore = vectorStore;
        this.vectorJdbcTemplate = vectorJdbcTemplate;
    }

    public int ingest(Resource mdFile, String docId, String dept, String title) {
        // 1. 结构化读取：标题成为 metadata，段落成为正文
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(true)
                .withIncludeBlockquote(true)
                .withAdditionalMetadata("source", docId)
                .withAdditionalMetadata("dept", dept)
                .withAdditionalMetadata("title", title)
                .withAdditionalMetadata("doc_type", "markdown")
                .build();

        List<Document> sections = new MarkdownDocumentReader(mdFile, config).get();

        // 2. 长度兜底：超长章节按 token 再切
        List<Document> chunks = new TokenTextSplitter()
                .apply(sections);

        // 3. 幂等：同 source 先删旧数据，重灌不产生重复
        deleteByDocId(docId);

        // 4. 向量化入库
        vectorStore.add(chunks);
        return chunks.size();
    }

    public void deleteByDocId(String docId) {
        Filter.Expression expression = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("source"),
                new Filter.Value(docId));
        vectorStore.delete(expression);
    }





}
