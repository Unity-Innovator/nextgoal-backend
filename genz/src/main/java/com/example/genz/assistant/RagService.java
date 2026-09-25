package com.example.genz.assistant;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "assistant.rag.enabled", havingValue = "true")
public class RagService {
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
            var vector = provider.embed(List.of(question)).getFirst();
            var sources = store.search(owner, vector, threshold);
            if (sources.isEmpty()) return fallback();
            return new AssistantService.Reply(provider.answer(question, sources), sources);
        } catch (RuntimeException ex) {
            throw new AssistantService.Unavailable();
        }
    }
    private static AssistantService.Reply fallback() {
        return new AssistantService.Reply("I do not have enough information in your uploaded documents to answer that question.", List.of());
    }
}
