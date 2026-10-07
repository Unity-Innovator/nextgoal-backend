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
    private KnowledgeBase knowledge;
    @org.springframework.beans.factory.annotation.Autowired
    void configureKnowledge(KnowledgeBase knowledge) { this.knowledge = knowledge; }
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
        var greeting = AssistantSmallTalk.reply(question);
        if (greeting != null) return greeting;
        try {
            String revision = store.cacheRevision(owner);
            var guideSources = knowledge == null ? List.<KnowledgeBase.Source>of() : knowledge.search(question);
            if (revision.isEmpty() && guideSources.isEmpty()) return fallback();
            if (cache == null) return retrieveAnswer(owner, question, !revision.isEmpty(), guideSources);
            // Shared uploads, deletion, and admin role changes alter every affected user's revision.
            String cacheKey = com.example.genz.common.cache.BackendResponseCache.key(
                    "rag-shared-answer", owner, revision, provider.cacheModel(), guideSources.toString(),
                    RagProvider.EMBEDDING_MODEL, Double.toString(threshold), question.trim());
            return cache.get(cacheKey, AssistantService.Reply.class, cacheTtl,
                    () -> retrieveAnswer(owner, question, !revision.isEmpty(), guideSources));
        } catch (RuntimeException ex) {
            if (ex instanceof AssistantService.Unavailable unavailable) throw unavailable;
            String reason = ex instanceof org.springframework.dao.DataAccessException
                    ? "DOCUMENT_STORE_ERROR" : "RETRIEVAL_ERROR";
            throw AssistantDiagnostics.unavailable("rag", "retrieval", reason);
        }
    }
    private AssistantService.Reply retrieveAnswer(String owner, String question, boolean hasDocuments,
                                                  List<KnowledgeBase.Source> guideSources) {
        List<KnowledgeBase.Source> sources = List.of();
        if (hasDocuments) {
            var vector = provider.embedQuery(question);
            sources = store.search(owner, vector, threshold);
        }
        // Include exact app-guide matches even when a loosely related upload was retrieved.
        sources = java.util.stream.Stream.concat(sources.stream(), guideSources.stream()).distinct().toList();
        if (sources.isEmpty()) return fallback();
        return new AssistantService.Reply(provider.answer(question, sources), sources);
    }
    private static AssistantService.Reply fallback() {
        return new AssistantService.Reply("I could not find that information in the available documents or app guide. Please try a more specific question.", List.of());
    }
}
