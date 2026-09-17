package com.api.aiagent.mapper;

import com.api.aiagent.entity.KnowledgeDocument;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;

public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument> {

    @Insert("""
            INSERT INTO KNOWLEDGE_DOCUMENT
                (doc_id, original_filename, title, dept, doc_type, chunk_count, file_size, deleted, deleted_at)
            VALUES (#{docId}, #{originalFilename}, #{title}, #{dept}, #{docType}, #{chunkCount}, #{fileSize}, 0, NULL)
            ON DUPLICATE KEY UPDATE
                original_filename = VALUES(original_filename),
                title = VALUES(title),
                dept = VALUES(dept),
                doc_type = VALUES(doc_type),
                chunk_count = VALUES(chunk_count),
                file_size = VALUES(file_size),
                deleted = 0,
                deleted_at = NULL
            """)
    int upsert(KnowledgeDocument document);
}
