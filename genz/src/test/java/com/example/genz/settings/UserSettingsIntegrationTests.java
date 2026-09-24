package com.example.genz.settings;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:genz_settings;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.show-sql=false",
    "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long"
})
@AutoConfigureMockMvc
@Transactional
class UserSettingsIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    private String account(String phone) {
        users.saveAndFlush(User.builder().fullName("Settings User").phoneNumber(phone)
            .dateOfBirth(LocalDate.of(2000, 1, 1)).gender(Gender.FEMALE).status("ACTIVE").build());
        return phone;
    }

    @Test
    void patchPersistsOnlyChangesAndKeepsAccountsSeparate() throws Exception {
        String alice = account("012990001");
        String bob = account("012990002");
        mvc.perform(get("/api/settings").with(user(alice)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.theme").value("LIGHT"));
        mvc.perform(patch("/api/settings").with(user(alice)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"theme\":\"DARK\",\"language\":\"km\",\"pushNotificationsEnabled\":false,\"profileVisibility\":\"FRIENDS_ONLY\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.theme").value("DARK"))
            .andExpect(jsonPath("$.data.pushNotificationsEnabled").value(false))
            .andExpect(jsonPath("$.data.billReminder").value(true));
        mvc.perform(get("/api/settings").with(user(alice)))
            .andExpect(jsonPath("$.data.language").value("km"))
            .andExpect(jsonPath("$.data.profileVisibility").value("FRIENDS_ONLY"));
        mvc.perform(get("/api/settings").with(user(bob)))
            .andExpect(jsonPath("$.data.theme").value("LIGHT"))
            .andExpect(jsonPath("$.data.pushNotificationsEnabled").value(true));
    }

    @Test
    void invalidSettingsAreRejectedWithoutChangingStoredValues() throws Exception {
        String phone = account("012990003");
        for (String invalid : new String[] {"{\"theme\":\"purple\"}", "{\"language\":\"fr\"}",
                "{\"autoLockMinutes\":0}", "{\"autoLockMinutes\":61}", "{\"preferredCurrency\":\"INVALID\"}"}) {
            mvc.perform(patch("/api/settings").with(user(phone)).contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/settings").with(user(phone)))
            .andExpect(jsonPath("$.data.theme").value("LIGHT"));
    }

    @Test
    void settingsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/settings")).andExpect(status().is4xxClientError());
    }
}
