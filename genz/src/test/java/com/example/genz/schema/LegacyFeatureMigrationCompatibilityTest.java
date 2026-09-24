package com.example.genz.schema;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyFeatureMigrationCompatibilityTest {

    @Test
    void v4CompletesPartialLegacyFeatureTablesAndCanBeRerun() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:legacy_feature_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        )) {
            createPartialLegacySchema(connection);

            executeV4(connection);
            executeV4(connection);

            Map<String, List<String>> expectedColumns = Map.of(
                    "users", List.of("energy", "total_xp", "energy_reset_date"),
                    "user_settings", List.of("daily_challenge_enabled"),
                    "badges", List.of("category", "xp_reward", "rarity", "active")
            );

            expectedColumns.forEach((table, columns) -> columns.forEach(column ->
                    assertTrue(columnExists(connection, table, column),
                            () -> table + "." + column + " should be created by V4")
            ));

            assertLegacyRowsWereBackfilled(connection);
        }
    }

    private void createPartialLegacySchema(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE users (
                        user_id UUID PRIMARY KEY,
                        energy INTEGER
                    )
                    """);
            statement.execute("""
                    CREATE TABLE user_settings (
                        settings_id UUID PRIMARY KEY,
                        user_id UUID NOT NULL,
                        daily_challenge_enabled BOOLEAN
                    )
                    """);
            statement.execute("""
                    CREATE TABLE badges (
                        badge_id UUID PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        requirement_type VARCHAR(50) NOT NULL,
                        requirement_value INTEGER,
                        category VARCHAR(50),
                        rarity VARCHAR(50)
                    )
                    """);

            statement.execute("""
                    INSERT INTO users (user_id)
                    VALUES ('00000000-0000-0000-0000-000000000001')
                    """);
            statement.execute("""
                    INSERT INTO user_settings (settings_id, user_id)
                    VALUES (
                        '00000000-0000-0000-0000-000000000010',
                        '00000000-0000-0000-0000-000000000001'
                    )
                    """);
            statement.execute("""
                    INSERT INTO badges (badge_id, name, requirement_type, requirement_value)
                    VALUES (
                        '00000000-0000-0000-0000-000000000020',
                        'Legacy badge',
                        'FIRST_GOAL_CREATED',
                        1
                    )
                    """);
        }
    }

    private void executeV4(Connection connection) throws Exception {
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/V4__reconcile_legacy_feature_columns.sql"
        )) {
            assertNotNull(stream, "V4 migration resource should exist");
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

    private void assertLegacyRowsWereBackfilled(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet user = statement.executeQuery(
                     "SELECT energy, total_xp FROM users"
             )) {
            assertTrue(user.next());
            assertEquals(100, user.getInt("energy"));
            assertEquals(0, user.getInt("total_xp"));
        }

        try (Statement statement = connection.createStatement();
             ResultSet settings = statement.executeQuery(
                     "SELECT daily_challenge_enabled FROM user_settings"
             )) {
            assertTrue(settings.next());
            assertTrue(settings.getBoolean("daily_challenge_enabled"));
        }

        try (Statement statement = connection.createStatement();
             ResultSet badge = statement.executeQuery(
                     "SELECT category, xp_reward, rarity, active FROM badges"
             )) {
            assertTrue(badge.next());
            assertEquals("GETTING_STARTED", badge.getString("category"));
            assertEquals(0, badge.getInt("xp_reward"));
            assertEquals("COMMON", badge.getString("rarity"));
            assertTrue(badge.getBoolean("active"));
        }
    }

    private boolean columnExists(Connection connection, String table, String column) {
        try (ResultSet result = connection.getMetaData().getColumns(
                null,
                null,
                table.toUpperCase(),
                column.toUpperCase()
        )) {
            return result.next();
        } catch (Exception exception) {
            throw new AssertionError("Could not inspect " + table + "." + column, exception);
        }
    }
}
