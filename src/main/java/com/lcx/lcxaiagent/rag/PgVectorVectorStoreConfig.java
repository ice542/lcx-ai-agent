//package com.lcx.lcxaiagent.rag;
//
//import jakarta.annotation.Resource;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.embedding.EmbeddingModel;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.jdbc.core.JdbcTemplate;
//
//import java.util.List;
//
//@Configuration
//public class PgVectorVectorStoreConfig {
//    @Resource
//    private LoveAppDocumentLoader loveAppDocumentLoader;
//    @Bean
//    public VectorStore pgVectorVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel dashscopeEmbeddingModel){
//        PgVectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
//                .dimensions(1536)
//                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
//                .indexType(PgVectorStore.PgIndexType.HNSW)
//                .initializeSchema(true)
//                .schemaName("public")
//                .vectorTableName("vector_store")
//                .maxDocumentBatchSize(10000)
//                .build();
//        List<Document> documents = loveAppDocumentLoader.loadMarkdowns();
//        vectorStore.add(documents);
//        return vectorStore;
//    }
//}
