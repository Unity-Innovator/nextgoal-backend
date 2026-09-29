package com.example.genz.assistant;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "assistant.rag.enabled", havingValue = "true")
public class RagService {
    private com.example.genz.common.cache.BackendResponseCache cache;
    private java.time.Duration cacheTtl = java.time.Duration.ofMinutes(2);

    @org.springframework.beans.factory.annotation.Autowired
    void configureCache(com.example.genz.common.cache.BackendResponseCache cache,
            @Value("${assistant.cache.ttl:PT2M}") java.time.Duration ttl) {
        this.cache = cache; this.cacheTtl = ttl;
    }
    private final RagStore store;
    private final RagProvider provider;
    private final DocumentExtractor extractor;
    private final double threshold;
    public RagService(RagStore store, RagProvider provider, DocumentExtractor extractor,
                      @Value("${assistant.rag.min-similarity:0.3}") double threshold) {
        if (!Double.isFinite(threshold) || threshold < -1 || threshold > 1)
            throw new IllegalArgumentException("Similarity must be between -1 and 1");
        this.store = store; this.provider = provider; this.extractor = extractor; this.threshold = threshold;
    }
    public RagStore.Document upload(String owner, MultipartFile file) {
        var document = extractor.extract(file);
        var vectors = provider.embed(document.chunks().stream().map(DocumentExtractor.Chunk::content).toList());
        return store.save(owner, document, vectors);
    }
    public AssistantService.Reply answer(String owner, String question) {
        try {
            if (store.list(owner).isEmpty()) return fallback();
            if (cache == null) return retrieveAnswer(owner, question);
            // Reading the committed corpus revision makes invalidation work across
            // instances, including when an upload/delete races an older answer.
            String cacheKey = com.example.genz.common.cache.BackendResponseCache.key(
                    "rag-answer", owner, store.cacheRevision(owner), provider.cacheModel(),
                    RagProvider.EMBEDDING_MODEL, Double.toString(threshold), question.trim());
            return cache.get(cacheKey, AssistantService.Reply.class, cacheTtl,
                    () -> retrieveAnswer(owner, question));
        } catch (RuntimeException ex) {
            if (ex instanceof AssistantService.Unavailable unavailable) throw unavailable;
            String reason = ex instanceof org.springframework.dao.DataAccessException
                    ? "DOCUMENT_STORE_ERROR" : "RETRIEVAL_ERROR";
            throw AssistantDiagnostics.unavailable("rag", "retrieval", reason);
        }
    }
    private AssistantService.Reply retrieveAnswer(String owner, String question) {
        var vector = provider.embedQuery(question);
        var sources = store.search(owner, vector, threshold);
        if (sources.isEmpty()) return fallback();
        return new AssistantService.Reply(provider.answer(question, sources), sources);
    }
    private static AssistantService.Reply fallback() {
        return new AssistantService.Reply("I do not have enough information in your uploaded documents to answer that question.", List.of());
    }
}
