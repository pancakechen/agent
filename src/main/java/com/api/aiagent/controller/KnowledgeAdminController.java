package com.api.aiagent.controller;


import com.api.aiagent.service.MarkdownIngestionService;
import com.api.aiagent.entity.KnowledgeDocument;
import com.api.aiagent.service.KnowledgeDocumentService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/knowledge")
public class KnowledgeAdminController {

    private final MarkdownIngestionService ingestionService;
    private final KnowledgeDocumentService documentService;

    public KnowledgeAdminController(MarkdownIngestionService ingestionService,
                                    KnowledgeDocumentService documentService) {
        this.ingestionService = ingestionService;
        this.documentService = documentService;
    }

    @PostMapping("/ingest")
    public Map<String, Object> ingest(@RequestParam("file") MultipartFile file,
                                      @RequestParam(defaultValue = "general") String dept,
                                      @RequestParam(required = false) String title) throws Exception {
        String name = file.getOriginalFilename();
        if (name == null || !(name.endsWith(".md") || name.endsWith(".markdown"))) {
            throw new IllegalArgumentException("MVP 阶段仅支持 Markdown 文件");
        }
        String docId = name;
        int chunks = ingestionService.ingest(
                new ByteArrayResource(file.getBytes()),
                docId,
                dept,
                title != null ? title : name.replaceAll("\\.markdown$", "").replaceAll("\\.md$", ""));
        String documentTitle = title != null ? title : name.replaceAll("\\.markdown$", "").replaceAll("\\.md$", "");
        documentService.saveUploadedDocument(docId, name, documentTitle, dept, chunks, file.getSize());

        return Map.of("docId", docId, "chunks", chunks, "status", "ingested");
    }

    @GetMapping("/documents")
    public List<KnowledgeDocument> listDocuments() {
        return documentService.listDocuments();
    }

    @DeleteMapping("/documents/{docId:.+}")
    public Map<String, Object> deleteDocument(@PathVariable String docId) {
        if (!documentService.deleteDocument(docId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文档不存在");
        }
        return Map.of("docId", docId, "status", "deleted");
    }
}
