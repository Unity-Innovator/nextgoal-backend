package com.example.genz.assistant;

import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssistantTests {
    @Test void generatesAnAnswerUsingRetrievedContext() throws Exception {
        var builder = org.springframework.web.client.RestClient.builder().baseUrl("https://api.groq.com/openai/v1");
        var server = org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://api.groq.com/openai/v1/chat/completions"))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("Authorization", "Bearer test-key"))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content().string(org.hamcrest.Matchers.containsString("Reference passages:")))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content().string(org.hamcrest.Matchers.containsString("registration.md-")))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                "{\"choices\":[{\"message\":{\"content\":\"Enter the six-digit code [registration.md-1].\"}}]}",
                org.springframework.http.MediaType.APPLICATION_JSON));
        var service = new AssistantService(new KnowledgeBase(), "test-key", "test-model", builder.build());
        var reply = service.answer("OTP code");
        assertTrue(reply.answer().contains("six-digit"));
        assertFalse(reply.sources().isEmpty());
        server.verify();
    }
    @Test void retrievesRelevantPassagesAndRejectsUnrelatedQuestions() throws Exception {
        var knowledge = new KnowledgeBase();
        assertTrue(knowledge.search("OTP verification code").stream().anyMatch(s -> s.id().startsWith("registration")));
        assertTrue(knowledge.search("withdrawals").stream().anyMatch(s -> s.id().startsWith("payments")));
        assertTrue(knowledge.search("quantum astrophysics").isEmpty());
        assertTrue(knowledge.search("how do I").isEmpty());
        assertTrue(knowledge.search("goals").size() <= 3);
    }

    @Test void unknownQuestionDoesNotNeedAProviderAndKnownQuestionRequiresKey() throws Exception {
        var service = new AssistantService(new KnowledgeBase(), "", "unused");
        assertTrue(service.answer("quantum astrophysics").sources().isEmpty());
        assertThrows(AssistantService.Unavailable.class, () -> service.answer("OTP code"));
    }

    @Test void validatesAuthenticationInputAndSuccessfulResponse() throws Exception {
        var service = mock(AssistantService.class);
        when(service.answer("goals")).thenReturn(new AssistantService.Reply("Use Goals", List.of()));
        var mvc = MockMvcBuilders.standaloneSetup(new AssistantController(service)).build();
        mvc.perform(post("/api/assistant/chat").contentType("application/json")
                .content("{\"message\":\"goals\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/assistant/chat").principal(() -> "user").contentType("application/json")
                .content("{\"message\":\" \"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/assistant/chat").principal(() -> "user").contentType("application/json")
                .content("{\"message\":\"" + "a".repeat(2001) + "\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/assistant/chat").principal(() -> "user").contentType("application/json")
                .content("{\"message\":\"goals\"}")).andExpect(status().isOk());
        verify(service, times(1)).answer("goals");
    }

    @Test void rateLimitsAndSanitizesProviderFailure() {
        var service = mock(AssistantService.class);
        when(service.answer(anyString())).thenThrow(new AssistantService.Unavailable());
        var controller = new AssistantController(service);
        Principal user = () -> "user";
        for (int i = 0; i < 10; i++)
            assertEquals(503, controller.chat(new AssistantController.Question("goals"), user).getStatusCode().value());
        assertEquals(429, controller.chat(new AssistantController.Question("goals"), user).getStatusCode().value());
        verify(service, times(10)).answer("goals");
    }
}
