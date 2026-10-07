package com.example.genz.assistant;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.UserRole;
import com.example.genz.auth.mapper.UserMapper;
import com.example.genz.security.config.SecurityConfig;
import com.example.genz.security.jwt.JwtFilter;
import com.example.genz.security.jwt.JwtUtil;
import com.example.genz.security.user.CustomUserDetailsService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(properties = "assistant.rag.enabled=true")
@ContextConfiguration(classes = {SecurityConfig.class, JwtFilter.class, RagDocumentController.class})
class AiDocumentAccessTests {
    @Autowired MockMvc mvc;
    @MockitoBean RagService service;
    @MockitoBean RagStore store;
    @MockitoBean JwtUtil jwt;
    @MockitoBean CustomUserDetailsService users;

    @Test void loginUserResponseIncludesRole() {
        var mapper = new UserMapper();
        assertEquals(UserRole.ADMIN, mapper.toUserResponse(User.builder().role(UserRole.ADMIN).build()).role());
        assertEquals(UserRole.USER, mapper.toUserResponse(User.builder().role(UserRole.USER).build()).role());
        assertEquals(UserRole.USER, mapper.toUserResponse(User.builder().role(null).build()).role());
    }

    @Test void profileIncludesCurrentDatabaseRole() {
        var mapper = new com.example.genz.user.mapper.ProfileMapper();
        var account = User.builder().role(UserRole.USER).build();
        assertEquals(UserRole.USER, mapper.toUserResponse(account).getRole());
        account.setRole(UserRole.ADMIN);
        assertEquals(UserRole.ADMIN, mapper.toUserResponse(account).getRole());
        account.setRole(UserRole.USER);
        assertEquals(UserRole.USER, mapper.toUserResponse(account).getRole());
    }

    @Test void regularUserCannotManageDocuments() throws Exception {
        mvc.perform(get("/api/assistant/documents").with(user("member").roles("USER")))
            .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/assistant/documents")
            .file(new MockMultipartFile("file", "guide.txt", "text/plain", "Guide".getBytes()))
            .with(user("member").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/assistant/documents/00000000-0000-0000-0000-000000000001")
            .with(user("member").roles("USER"))).andExpect(status().isForbidden());
        verifyNoInteractions(service, store);
    }

    @Test void adminCanListUploadAndDeleteOwnDocuments() throws Exception {
        when(store.list("admin")).thenReturn(List.of());
        mvc.perform(get("/api/assistant/documents").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk());
        mvc.perform(multipart("/api/assistant/documents")
            .file(new MockMultipartFile("file", "guide.txt", "text/plain", "Guide".getBytes()))
            .with(user("admin").roles("ADMIN"))).andExpect(status().isCreated());
        verify(service).upload(eq("admin"), any());
        var id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(store.delete("admin", id)).thenReturn(true);
        mvc.perform(delete("/api/assistant/documents/" + id).with(user("admin").roles("ADMIN")))
            .andExpect(status().isNoContent());
        verify(store).delete("admin", id);
    }

    @Test void anonymousCannotUploadDocuments() throws Exception {
        mvc.perform(multipart("/api/assistant/documents")
            .file(new MockMultipartFile("file", "guide.txt", "text/plain", "Guide".getBytes())))
            .andExpect(status().is4xxClientError());
        verifyNoInteractions(service, store);
    }
}