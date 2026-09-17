CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    conversation_id VARCHAR(36)  NOT NULL COMMENT '会话ID(UUID)',
    content         TEXT         NOT NULL COMMENT '消息内容',
    type            VARCHAR(10)  NOT NULL COMMENT '消息类型',
    timestamp       TIMESTAMP    NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_conv_ts (conversation_id, timestamp)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='Spring AI 会话记忆持久化表';

CREATE TABLE IF NOT EXISTS KNOWLEDGE_DOCUMENT
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    doc_id            VARCHAR(255) NOT NULL COMMENT '文档ID，对应向量库metadata.source',
    original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    title             VARCHAR(255) NOT NULL COMMENT '文档标题',
    dept              VARCHAR(64)  NOT NULL COMMENT '部门/知识分类',
    doc_type          VARCHAR(32)  NOT NULL DEFAULT 'markdown' COMMENT '文档类型',
    chunk_count       INT          NOT NULL DEFAULT 0 COMMENT '切片数量',
    file_size         BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小，单位字节',
    deleted           TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否删除：0否，1是',
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at        TIMESTAMP    NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_doc_id (doc_id),
    INDEX idx_deleted_updated (deleted, updated_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='知识库上传文档记录表';
