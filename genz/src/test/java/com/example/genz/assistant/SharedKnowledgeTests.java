package com.example.genz.assistant;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SharedKnowledgeTests {
    @Test void existingAdminUploadsAreVisibleAndOtherUsersPrivateDocumentsAreNot() {
        var db = new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        try {
            var jdbc = new JdbcTemplate(db);
            jdbc.execute("CREATE TABLE users (phone_number VARCHAR PRIMARY KEY, role VARCHAR)");
            jdbc.execute("CREATE TABLE assistant_documents (id UUID PRIMARY KEY, owner_id VARCHAR)");
            jdbc.update("INSERT INTO users VALUES ('admin', 'ADMIN'), ('alice', 'USER'), ('bob', 'USER')");
            var shared = UUID.randomUUID(); var privateDoc = UUID.randomUUID();
            jdbc.update("INSERT INTO assistant_documents VALUES (?, 'admin'), (?, 'alice')", shared, privateDoc);
            var store = new RagStore(jdbc);
            assertEquals(shared.toString(), store.cacheRevision("bob"));
            assertTrue(store.cacheRevision("alice").contains(shared.toString()));
            assertTrue(store.cacheRevision("alice").contains(privateDoc.toString()));
            assertFalse(store.delete("bob", shared));
            jdbc.update("UPDATE users SET role='USER' WHERE phone_number='admin'");
            assertEquals("", store.cacheRevision("bob"));
            jdbc.update("UPDATE users SET role='ADMIN' WHERE phone_number='admin'");
            assertEquals(shared.toString(), store.cacheRevision("bob"));
            assertTrue(store.delete("admin", shared));
            assertEquals("", store.cacheRevision("bob"));
            assertEquals(privateDoc.toString(), store.cacheRevision("alice"));
        } finally { db.shutdown(); }
    }

    @Test void userWithNoOwnUploadsReceivesAnswerFromSharedDocument() {
        var store = mock(RagStore.class); var provider = mock(RagProvider.class);
        var sources = List.of(new KnowledgeBase.Source("shared", "Admin guide", "The support desk opens at 9am."));
        when(store.cacheRevision("member")).thenReturn("shared");
        when(provider.embedQuery("Opening time?")).thenReturn("vector");
        when(store.search("member", "vector", 0.3)).thenReturn(sources);
        when(provider.answer("Opening time?", sources)).thenReturn("9am.");
        var service = new RagService(store, provider, new DocumentExtractor(), 0.3);
        assertEquals("9am.", service.answer("member", "Opening time?").answer());
        verify(store, never()).list(anyString());
    }

    @Test void greetingsDoNotNeedUploadsOrProviderAndQuestionsAreNotSwallowed() {
        var store = mock(RagStore.class); var provider = mock(RagProvider.class);
        var service = new RagService(store, provider, new DocumentExtractor(), 0.3);
        for (String greeting : List.of("hii", "Hello!", "GOOD MORNING", "thanks", "bye", "សួស្តី")) {
            assertFalse(service.answer("member", greeting).answer().isBlank());
        }
        assertNull(AssistantSmallTalk.reply("hi, how to login?"));
        assertNull(AssistantSmallTalk.reply("thanks, but how can I reset my password?"));
        verifyNoInteractions(store, provider);
    }

    @Test void loginUsesAppGuideWithoutUploads() throws Exception {
        var store = mock(RagStore.class); var provider = mock(RagProvider.class);
        when(store.cacheRevision("member")).thenReturn("");
        when(provider.answer(eq("how to login?"), anyList())).thenReturn("Use your registered phone number and password.");
        var service = new RagService(store, provider, new DocumentExtractor(), 0.3);
        service.configureKnowledge(new KnowledgeBase());
        var reply = service.answer("member", "how to login?");
        assertTrue(reply.sources().stream().anyMatch(source -> source.id().startsWith("login.md")));
        assertTrue(reply.answer().contains("phone number"));
        verify(provider, never()).embedQuery(anyString());
    }

    @Test void appGuideStillAnswersLoginWhenGreetingDocumentIsRetrieved() throws Exception {
        var store = mock(RagStore.class); var provider = mock(RagProvider.class);
        when(store.cacheRevision("member")).thenReturn("shared");
        when(provider.embedQuery("how to login?")).thenReturn("vector");
        when(store.search("member", "vector", 0.3)).thenReturn(List.of(new KnowledgeBase.Source("greeting", "Greeting guide", "Say hello warmly.")));
        when(provider.answer(eq("how to login?"), anyList())).thenReturn("Use the sign-in screen.");
        var service = new RagService(store, provider, new DocumentExtractor(), 0.3);
        service.configureKnowledge(new KnowledgeBase());
        assertTrue(service.answer("member", "how to login?").sources().stream()
                .anyMatch(source -> source.id().startsWith("login.md")));
    }
}
