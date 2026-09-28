package com.example.genz.assistant;

import com.example.genz.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
    public record Question(@NotBlank @Size(max = 2000) String message) {}
    private record Window(long minute, int count) {}
    private final Map<String, Window> windows = new HashMap<>();
    private final AssistantService service;
    private final RagService rag;
    public AssistantController(AssistantService service) { this.service = service; this.rag = null; }
    @org.springframework.beans.factory.annotation.Autowired
    public AssistantController(AssistantService service, org.springframework.beans.factory.ObjectProvider<RagService> rag) {
        this.service = service;
        this.rag = rag.getIfAvailable();
    }

    private synchronized boolean allow(String user) {
        long minute = Instant.now().getEpochSecond() / 60;
        windows.entrySet().removeIf(e -> e.getValue().minute() != minute);
        Window prior = windows.get(user);
        if (prior != null && prior.count() >= 10) return false;
        if (prior == null && windows.size() >= 10000) return false;
        windows.put(user, new Window(minute, prior == null ? 1 : prior.count() + 1));
        return true;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@Valid @RequestBody Question request, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(ApiResponse.error("Sign in required"));
        if (!allow(principal.getName())) return ResponseEntity.status(429).body(ApiResponse.error("Please try again in a minute"));
        try {
            var reply = rag == null ? service.answer(request.message().trim())
                    : rag.answer(principal.getName(), request.message().trim());
            return ResponseEntity.ok(ApiResponse.success("Answer generated", reply));
        } catch (AssistantService.Unavailable ex) {
            return ResponseEntity.status(503).body(ApiResponse.error("Assistant unavailable"));
        }
    }
}
