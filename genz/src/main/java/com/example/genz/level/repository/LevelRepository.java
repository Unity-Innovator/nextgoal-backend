package com.example.genz.level.repository;

import com.example.genz.level.entity.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LevelRepository extends JpaRepository<Level, UUID> {

    List<Level> findAllByOrderByLevelNumberAsc();

    Optional<Level> findByLevelNumber(Integer levelNumber);
}
