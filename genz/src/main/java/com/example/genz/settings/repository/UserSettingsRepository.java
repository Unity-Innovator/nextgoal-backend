package com.example.genz.settings.repository;

import com.example.genz.settings.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.genz.auth.entity.User;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, UUID> {

    Optional<UserSettings> findByUser(User user);

    Optional<UserSettings> findByUserId(UUID userId);

    Optional<UserSettings> findByUser_Id(UUID userId);
}
