package com.api.aiagent.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    // ===== 主数据源：MySQL（会话记忆 + 业务数据）=====

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties mysqlDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        return mysqlDataSourceProperties().initializeDataSourceBuilder().build();
    }

    // 必须显式定义主库 JdbcTemplate 并标 @Primary，
    // 否则 ChatMemoryConfig 会拿到下面 PG 的那个
    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("dataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // ===== 第二数据源：PostgreSQL（向量库专用）=====

    @Bean
    @ConfigurationProperties("app.vector")
    public DataSourceProperties vectorDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource vectorDataSource() {
        return vectorDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    public JdbcTemplate vectorJdbcTemplate(
            @Qualifier("vectorDataSource") DataSource vectorDataSource) {
        return new JdbcTemplate(vectorDataSource);
    }

    // ===== PGVector 向量库 =====

    @Bean
    public VectorStore vectorStore(
            @Qualifier("vectorJdbcTemplate") JdbcTemplate vectorJdbcTemplate,
            EmbeddingModel embeddingModel) {

        return PgVectorStore.builder(vectorJdbcTemplate, embeddingModel)
                .vectorTableName("vector_store")
                .dimensions(1024)                          // 必须等于 embedding-3 的输出维度
                .indexType(PgVectorStore.PgIndexType.HNSW) // ANN 索引
                .initializeSchema(true)                    // 启动时自动建表
                .build();
    }
}
