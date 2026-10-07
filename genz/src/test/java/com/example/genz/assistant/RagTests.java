package com.example.genz.assistant;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RagTests {
    @Test void chunksOverlapAndPreservePdfPages() throws Exception {
        var chunks = new ArrayList<DocumentExtractor.Chunk>();
        DocumentExtractor.add(chunks, "a".repeat(5000), 7);
        assertEquals(3, chunks.size());
        assertEquals(2400, chunks.getFirst().content().length());
        assertEquals(7, chunks.getFirst().pageNumber());
        assertEquals(chunks.get(0).content().substring(2200), chunks.get(1).content().substring(0,200));
        try (var pdf = new org.apache.pdfbox.pdmodel.PDDocument()) {
            var page = new org.apache.pdfbox.pdmodel.PDPage();
            pdf.addPage(page);
            try (var content = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf, page)) {
                content.beginText();
                content.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(
                    org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700); content.showText("Annual leave is eighteen days."); content.endText();
            }
            var bytes = new ByteArrayOutputStream(); pdf.save(bytes);
            var result = new DocumentExtractor().extract(new MockMultipartFile("file", "policy.pdf", "application/pdf", bytes.toByteArray()));
            assertEquals(1, result.chunks().getFirst().pageNumber());
            assertTrue(result.chunks().getFirst().content().contains("eighteen"));
        }
    }
    private MockMultipartFile docx(String xml) throws Exception {
        var output = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(xml.getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
        }
        return new MockMultipartFile("file", "policy.docx", "application/octet-stream", output.toByteArray());
    }
    @Test void extractsDocxAndRejectsExternalEntitiesAndInvalidFiles() throws Exception {
        var extractor = new DocumentExtractor();
        var file = docx("<w:document xmlns:w=\"urn:test\"><w:p><w:r><w:t>Refund period: 30 days</w:t></w:r></w:p></w:document>");
        assertEquals("Refund period: 30 days", extractor.extract(file).chunks().getFirst().content());
        var attack = docx("<!DOCTYPE x [<!ENTITY leak SYSTEM \"file:///private.txt\">]><x><t>&leak;</t></x>");
        assertThrows(IllegalArgumentException.class, () -> extractor.extract(attack));
        assertThrows(IllegalArgumentException.class, () -> extractor.extract(new MockMultipartFile("file","a.txt","text/plain",new byte[0])));
        assertThrows(IllegalArgumentException.class, () -> extractor.extract(new MockMultipartFile("file","a.exe","text/plain","hello".getBytes())));
        assertThrows(IllegalArgumentException.class, () -> extractor.extract(new MockMultipartFile("file","a.pdf","application/pdf","fake".getBytes())));
    }
    @Test void validatesGeminiEmbeddingsAndQueryTask() {
        var builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");
        var server = MockRestServiceServer.bindTo(builder).build();
        String vector = "[" + String.join(",", Collections.nCopies(1536, "0.1")) + "]";
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:batchEmbedContents";
        server.expect(requestTo(url)).andExpect(header("x-goog-api-key", "key"))
            .andExpect(jsonPath("$.requests[0].taskType").value("RETRIEVAL_DOCUMENT"))
            .andExpect(jsonPath("$.requests[0].outputDimensionality").value(1536))
            .andRespond(withSuccess("{\"embeddings\":[{\"values\":" + vector + "}]}", MediaType.APPLICATION_JSON));
        var provider = new RagProvider("key", "gemini-3.5-flash", builder.build());
        assertEquals(vector, provider.embed(List.of("policy")).getFirst());
        server.verify(); server.reset();
        server.expect(requestTo(url))
            .andExpect(jsonPath("$.requests[0].taskType").value("RETRIEVAL_QUERY"))
            .andRespond(withSuccess("{\"embeddings\":[{\"values\":" + vector + "}]}", MediaType.APPLICATION_JSON));
        assertEquals(vector, provider.embedQuery("policy?"));
        server.verify(); server.reset();
        server.expect(requestTo(url))
            .andRespond(withSuccess("{\"embeddings\":[{\"values\":[0.1]}]}", MediaType.APPLICATION_JSON));
        assertThrows(AssistantService.Unavailable.class, () -> provider.embed(List.of("policy")));
        server.verify();
    }
    @Test void geminiUsesGroundingAndExcludesThoughtParts() {
        var builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"))
            .andExpect(header("x-goog-api-key", "key"))
            .andExpect(jsonPath("$.systemInstruction.parts[0].text").exists())
            .andExpect(jsonPath("$.contents[0].parts[0].text").value(org.hamcrest.Matchers.containsString("page 14")))
            .andRespond(withSuccess("""
                {"candidates":[{"finishReason":"STOP","content":{"parts":[
                {"thought":true,"text":"hidden"},{"text":"18 days [s1]."}]}}]}
                """, MediaType.APPLICATION_JSON));
        assertEquals("18 days [s1].", new RagProvider("key","gemini-3.5-flash",builder.build())
            .answer("Leave?", List.of(new KnowledgeBase.Source("s1","Policy","18 days",14))));
        server.verify();
    }
    @Test void geminiRejectsBlockedTruncatedAndEmptyResponses() {
        for (String response : List.of("{}",
                "{\"candidates\":[{\"finishReason\":\"MAX_TOKENS\"}]}",
                "{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}",
                "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[]}}]}")) {
            var builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");
            var server = MockRestServiceServer.bindTo(builder).build();
            server.expect(anything()).andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
            assertThrows(AssistantService.Unavailable.class, () ->
                new RagProvider("key", "gemini-3.5-flash", builder.build()).answer("q", List.of()));
            server.verify();
        }
    }
    @Test void emptyKnowledgeSkipsProviderAndRetrievalUsesAuthenticatedOwner() {
        var store = mock(RagStore.class); var provider = mock(RagProvider.class);
        var service = new RagService(store,provider,new DocumentExtractor(),0.3);
        when(store.cacheRevision("alice")).thenReturn("");
        assertTrue(service.answer("alice","policy").sources().isEmpty());
        verifyNoInteractions(provider);
        when(store.cacheRevision("alice")).thenReturn("shared-document");
        when(provider.embedQuery("policy")).thenReturn("[0.1]");
        when(store.search("alice","[0.1]",0.3)).thenReturn(List.of());
        assertTrue(service.answer("alice","policy").sources().isEmpty());
        verify(store).search("alice","[0.1]",0.3);
        verify(provider,never()).answer(anyString(),anyList());
    }
    @Test void indexingFailureNeverPersistsPartialDocument() {
        var store = mock(RagStore.class); var provider = mock(RagProvider.class);
        when(provider.embed(anyList())).thenThrow(new AssistantService.Unavailable());
        var service = new RagService(store,provider,new DocumentExtractor(),0.3);
        assertThrows(AssistantService.Unavailable.class, () -> service.upload("alice",
            new MockMultipartFile("file","p.txt","text/plain","policy".getBytes())));
        verifyNoInteractions(store);
    }
    @Test void documentRoutesRequireAuthenticationAndUseOwnerForDeletion() {
        var store = mock(RagStore.class);
        var controller = new RagDocumentController(mock(RagService.class),store);
        UUID id = UUID.randomUUID();
        assertEquals(401,controller.list(null).getStatusCode().value());
        assertEquals(401,controller.delete(id,null).getStatusCode().value());
        assertEquals(401,controller.upload(null,null).getStatusCode().value());
        assertEquals(404,controller.delete(id,() -> "bob").getStatusCode().value());
        verify(store).delete("bob",id);
    }
}
