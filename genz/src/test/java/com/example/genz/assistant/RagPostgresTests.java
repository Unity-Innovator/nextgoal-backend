package com.example.genz.assistant;

import java.sql.DriverManager;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "RAG_TEST_DATABASE_URL", matches = ".+")
class RagPostgresTests {
    @Test void realVectorRankingOwnershipCascadeAndRollback() throws Exception {
        String schema = "rag_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(System.getenv("RAG_TEST_DATABASE_URL"),
                System.getenv().getOrDefault("RAG_TEST_DATABASE_USER","postgres"),
                System.getenv().getOrDefault("RAG_TEST_DATABASE_PASSWORD",""))) {
            var source = new SingleConnectionDataSource(connection, true);
            var jdbc = new JdbcTemplate(source);
            // Use a disposable database with pgvector installed in public.
            jdbc.execute("CREATE SCHEMA " + schema);
            try {
                jdbc.execute("SET search_path TO " + schema + ", public");
                new ResourceDatabasePopulator(new ClassPathResource("db/rag/V22__assistant_vector_knowledge.sql"),
                        new ClassPathResource("db/rag/V23__assistant_embedding_model.sql"))
                    .execute(source);
                var store = new RagStore(jdbc);
                var extracted = new DocumentExtractor.Extracted("policy.txt","txt",
                    List.of(new DocumentExtractor.Chunk("Annual leave is 18 days.", null)));
                String vector = "[1," + String.join(",",Collections.nCopies(1535,"0")) + "]";
                var alice = store.save("alice",extracted,List.of(vector));
                store.save("bob",extracted,List.of(vector));
                assertEquals(1,store.search("alice",vector,0.3).size());
                assertEquals(0,store.search("charlie",vector,0.3).size());
                assertFalse(store.delete("bob",alice.id()));
                assertEquals(1,store.list("alice").size());
                var transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
                assertThrows(RuntimeException.class, () -> transaction.execute(status ->
                    store.save("alice",extracted,List.of("[1,2]"))));
                assertEquals(1,store.list("alice").size());
                jdbc.update("UPDATE assistant_documents SET embedding_model=? WHERE id=?", "text-embedding-3-small", alice.id());
                assertTrue(store.search("alice",vector,0.3).isEmpty());
                assertEquals(1, store.list("alice").size());
                assertTrue(store.delete("alice",alice.id()));
                assertTrue(store.search("alice",vector,0.3).isEmpty());
                assertEquals(1,store.search("bob",vector,0.3).size());
            } finally {
                jdbc.execute("SET search_path TO public");
                jdbc.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }
}
