package com.example.genz.assistant;

import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(name = "assistant.rag.enabled", havingValue = "true")
public class RagStore {
    public record Document(UUID id, String name, String fileType, int chunkCount) {}
    private final JdbcTemplate jdbc;
    public RagStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Transactional
    public Document save(String owner, DocumentExtractor.Extracted extracted, List<String> vectors) {
        if (vectors.size() != extracted.chunks().size()) throw new IllegalArgumentException("Embedding count mismatch");
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO assistant_documents(id,owner_id,name,file_type,chunk_count,embedding_model) VALUES (?,?,?,?,?,?)",
                id, owner, extracted.name(), extracted.type(), vectors.size(), RagProvider.EMBEDDING_MODEL);
        for (int i = 0; i < vectors.size(); i++) {
            var chunk = extracted.chunks().get(i);
            jdbc.update("""
                INSERT INTO assistant_document_chunks(id,document_id,content,embedding,page_number,chunk_index)
                VALUES (?,?,?,CAST(? AS vector),?,?)
                """, UUID.randomUUID(), id, chunk.content(), vectors.get(i), chunk.pageNumber(), i);
        }
        return new Document(id, extracted.name(), extracted.type(), vectors.size());
    }
    public List<Document> list(String owner) {
        return jdbc.query("""
            SELECT id,name,file_type,chunk_count FROM assistant_documents
            WHERE owner_id=? ORDER BY created_at DESC LIMIT 100
            """, (rs, row) -> new Document(rs.getObject("id", UUID.class), rs.getString("name"),
                rs.getString("file_type"), rs.getInt("chunk_count")), owner);
    }
    public boolean delete(String owner, UUID id) {
        return jdbc.update("DELETE FROM assistant_documents WHERE id=? AND owner_id=?", id, owner) > 0;
    }
    public List<KnowledgeBase.Source> search(String owner, String vector, double threshold) {
        return jdbc.query("""
            WITH owned AS MATERIALIZED (
                SELECT c.id,c.content,c.page_number,c.embedding,d.name
                FROM assistant_document_chunks c
                JOIN assistant_documents d ON d.id=c.document_id WHERE d.owner_id=? AND d.embedding_model=?
            )
            SELECT id,content,page_number,name FROM owned
            WHERE 1 - (embedding <=> CAST(? AS vector)) >= ?
            ORDER BY embedding <=> CAST(? AS vector),id LIMIT 5
            """, (rs, row) -> new KnowledgeBase.Source(rs.getString("id"), rs.getString("name"),
                rs.getString("content"), rs.getObject("page_number", Integer.class)),
                owner, RagProvider.EMBEDDING_MODEL, vector, threshold, vector);
    }
}
