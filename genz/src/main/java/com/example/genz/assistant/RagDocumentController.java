package com.example.genz.assistant;

import com.example.genz.common.api.ApiResponse;
import java.security.Principal;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/assistant/documents")
@ConditionalOnProperty(name = "assistant.rag.enabled", havingValue = "true")
public class RagDocumentController {
    private final RagService service;
    private final RagStore store;
    private final Semaphore uploads = new Semaphore(2);
    public RagDocumentController(RagService service, RagStore store) { this.service = service; this.store = store; }
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(ApiResponse.error("Sign in required"));
        if (!uploads.tryAcquire()) return ResponseEntity.status(429).body(ApiResponse.error("Uploads busy; try again shortly"));
        try {
            return ResponseEntity.status(201).body(ApiResponse.success("Document indexed", service.upload(principal.getName(), file)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(503).body(ApiResponse.error("Document indexing unavailable"));
        } finally { uploads.release(); }
    }
    @GetMapping
    public ResponseEntity<?> list(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(ApiResponse.error("Sign in required"));
        return ResponseEntity.ok(ApiResponse.success("Documents", store.list(principal.getName())));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(ApiResponse.error("Sign in required"));
        if (!store.delete(principal.getName(), id)) return ResponseEntity.status(404).body(ApiResponse.error("Document not found"));
        return ResponseEntity.noContent().build();
    }
}
