package com.example.genz.auth.config;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.test-user-enabled", havingValue = "true")
public class DataSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedDatabase() {
        return args -> {

            if (userRepository.findByPhoneNumber("0946356476").isEmpty()) {

                User user = new User();
                user.setFullName("Test User");
                user.setPhoneNumber("0946356476");
                user.setGender(Gender.OTHER);
                user.setPasswordHash(passwordEncoder.encode("123456"));
                user.setStatus("ACTIVE");
                user.setDateOfBirth(LocalDate.of(1989, 12, 12));
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());

                userRepository.save(user);

                System.out.println("Test user created.");
            }
        };
    }
}
