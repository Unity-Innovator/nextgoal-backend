package com.example.genz.assistant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeBase {
    public record Source(String id, String title, String excerpt, Integer pageNumber) {
        public Source(String id, String title, String excerpt) { this(id, title, excerpt, null); }
    }
    private final List<Source> documents;
    private static final Set<String> STOP = Set.of("the","a","an","is","are","i","my","me","to","how","do","does","can","of","and","for","in","it","what","with","please");
    public KnowledgeBase() throws IOException {
        var result = new ArrayList<Source>();
        for (var resource : new PathMatchingResourcePatternResolver().getResources("classpath*:assistant/knowledge/*.md")) {
            String text = resource.getContentAsString(StandardCharsets.UTF_8).trim();
            String[] lines = text.split("\\R", 2);
            String title = lines[0].replaceFirst("^#+\\s*", "");
            String body = lines.length > 1 ? lines[1].trim() : "";
            int part = 0;
            for (String paragraph : body.split("\\R\\s*\\R")) {
                // Bounded passages keep retrieval context small.
                for (int start = 0; start < paragraph.length(); start += 1200) {
                    result.add(new Source(resource.getFilename() + "-" + part++, title,
                            paragraph.substring(start, Math.min(start + 1200, paragraph.length()))));
                }
            }
        }
        documents = List.copyOf(result);
    }
    private Set<String> terms(String text) {
        var terms = new HashSet<String>();
        for (String token : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (token.length() > 1 && !STOP.contains(token)) terms.add(token);
        }
        return terms;
    }
    public List<Source> search(String question) {
        Set<String> query = terms(question);
        if (query.isEmpty()) return List.of();
        record Hit(Source source, double score) {}
        return documents.stream().map(doc -> {
            Set<String> words = terms(doc.title() + " " + doc.excerpt());
            double score = query.stream().filter(words::contains).mapToDouble(term -> {
                long frequency = documents.stream().filter(d -> terms(d.title() + " " + d.excerpt()).contains(term)).count();
                return Math.log(1 + (double) documents.size() / (1 + frequency));
            }).sum();
            return new Hit(doc, score);
        }).filter(hit -> hit.score() > 0)
          .sorted(Comparator.comparingDouble(Hit::score).reversed().thenComparing(h -> h.source().id()))
          .limit(3).map(Hit::source).toList();
    }
}
