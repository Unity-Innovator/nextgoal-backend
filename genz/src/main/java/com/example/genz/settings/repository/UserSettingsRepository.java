package com.example.genz.settings.repository;

import com.example.genz.settings.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserSettingsRepository extends JpaRepository<UserSettings, UUID> {

    Optional<UserSettings> findByUser_Id(UUID userId);
}
