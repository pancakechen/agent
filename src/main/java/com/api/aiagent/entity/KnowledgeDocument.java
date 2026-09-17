package com.api.aiagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("KNOWLEDGE_DOCUMENT")
public class KnowledgeDocument {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("doc_id")
    private String docId;

    @TableField("original_filename")
    private String originalFilename;

    private String title;

    private String dept;

    @TableField("doc_type")
    private String docType;

    @TableField("chunk_count")
    private Integer chunkCount;

    @TableField("file_size")
    private Long fileSize;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
