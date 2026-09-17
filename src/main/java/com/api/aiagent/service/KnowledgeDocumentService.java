package com.api.aiagent.service;

import com.api.aiagent.entity.KnowledgeDocument;
import com.api.aiagent.mapper.KnowledgeDocumentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KnowledgeDocumentService {

    private final KnowledgeDocumentMapper documentMapper;
    private final MarkdownIngestionService ingestionService;

    public KnowledgeDocumentService(KnowledgeDocumentMapper documentMapper,
                                    MarkdownIngestionService ingestionService) {
        this.documentMapper = documentMapper;
        this.ingestionService = ingestionService;
    }

    public void saveUploadedDocument(String docId,
                                     String originalFilename,
                                     String title,
                                     String dept,
                                     int chunkCount,
                                     long fileSize) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setDocId(docId);
        document.setOriginalFilename(originalFilename);
        document.setTitle(title);
        document.setDept(dept);
        document.setDocType("markdown");
        document.setChunkCount(chunkCount);
        document.setFileSize(fileSize);
        documentMapper.upsert(document);
    }

    public List<KnowledgeDocument> listDocuments() {
        return documentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocument>()
                .orderByDesc(KnowledgeDocument::getUpdatedAt));
    }

    public boolean deleteDocument(String docId) {
        KnowledgeDocument document = documentMapper.selectOne(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getDocId, docId));
        if (document == null) {
            return false;
        }

        ingestionService.deleteByDocId(docId);

        document.setDeletedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        return documentMapper.deleteById(document.getId()) > 0;
    }
}
