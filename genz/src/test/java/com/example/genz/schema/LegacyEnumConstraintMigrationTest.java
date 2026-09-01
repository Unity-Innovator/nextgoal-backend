package com.example.genz.schema;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LegacyEnumConstraintMigrationTest {

    @Test
    void v6RemovesStaleBadgeEnumChecksAndCanBeRerun() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:legacy_enum_constraints;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        )) {
            createLegacyBadgeTable(connection);

            executeV6(connection);
            executeV6(connection);

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        INSERT INTO badges (badge_id, category, rarity, requirement_type)
                        VALUES (
                            '00000000-0000-0000-0000-000000000001',
                            'GETTING_STARTED',
                            'COMMON',
                            'FIRST_GOAL_CREATED'
                        )
                        """);

                try (ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM badges")) {
                    rows.next();
                    assertEquals(1, rows.getInt(1));
                }
            }
        }
    }

    private void createLegacyBadgeTable(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE badges (
                        badge_id UUID PRIMARY KEY,
                        category VARCHAR(50) NOT NULL,
                        rarity VARCHAR(50) NOT NULL,
                        requirement_type VARCHAR(50) NOT NULL,
                        CONSTRAINT badges_category_check
                            CHECK (category IN ('SAVING_STREAKS')),
                        CONSTRAINT badges_rarity_check
                            CHECK (rarity IN ('RARE')),
                        CONSTRAINT badges_requirement_type_check
                            CHECK (requirement_type IN ('STREAK_DAYS'))
                    )
                    """);
        }
    }

    private void executeV6(Connection connection) throws Exception {
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/V6__drop_legacy_enum_check_constraints.sql"
        )) {
            assertNotNull(stream, "V6 migration resource should exist");
            String migration = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            try (Statement statement = connection.createStatement()) {
                for (String sql : migration.split(";")) {
                    if (!sql.isBlank()) {
                        statement.execute(sql);
                    }
                }
            }
        }
    }
}
