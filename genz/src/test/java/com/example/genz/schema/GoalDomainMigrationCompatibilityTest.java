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

class GoalDomainMigrationCompatibilityTest {

    @Test
    void v5CompletesLegacyGoalSchemaAndCanBeRerun() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:goal_domain_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        )) {
            createPartialLegacySchema(connection);

            executeV5(connection);
            executeV5(connection);

            Map<String, List<String>> expectedColumns = Map.of(
                    "goals", List.of("goal_type"),
                    "saving_plans", List.of(
                            "plan_id", "goal_id", "contribution_amount", "frequency",
                            "next_contribution_date", "status", "contributed_this_period",
                            "created_at", "updated_at"
                    ),
                    "notifications", List.of(
                            "notification_id", "user_id", "title", "message", "type",
                            "reference_id", "is_read", "created_at"
                    )
            );

            expectedColumns.forEach((table, columns) -> columns.forEach(column ->
                    assertTrue(columnExists(connection, table, column),
                            () -> table + "." + column + " should be created by V5")
            ));

            assertLegacyGoalWasBackfilled(connection);
            assertTrue(indexExists(connection, "notifications", "ix_notifications_user_created"));
        }
    }

    private void createPartialLegacySchema(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE users (user_id UUID PRIMARY KEY)");
            statement.execute("""
                    CREATE TABLE goals (
                        goal_id UUID PRIMARY KEY,
                        goal_name VARCHAR(100) NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE notifications (
                        notification_id UUID PRIMARY KEY,
                        user_id UUID
                    )
                    """);

            statement.execute("""
                    INSERT INTO users (user_id)
                    VALUES ('00000000-0000-0000-0000-000000000001')
                    """);
            statement.execute("""
                    INSERT INTO goals (goal_id, goal_name)
                    VALUES (
                        '00000000-0000-0000-0000-000000000010',
                        'Legacy goal'
                    )
                    """);
        }
    }

    private void executeV5(Connection connection) throws Exception {
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/V5__reconcile_goal_domain_schema.sql"
        )) {
            assertNotNull(stream, "V5 migration resource should exist");
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

    private void assertLegacyGoalWasBackfilled(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT goal_type FROM goals")) {
            assertTrue(result.next());
            assertEquals("FLEXIBLE", result.getString("goal_type"));
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

    private boolean indexExists(Connection connection, String table, String index) {
        try (ResultSet result = connection.getMetaData().getIndexInfo(
                null,
                null,
                table.toUpperCase(),
                false,
                false
        )) {
            while (result.next()) {
                if (index.equalsIgnoreCase(result.getString("INDEX_NAME"))) {
                    return true;
                }
            }
            return false;
        } catch (Exception exception) {
            throw new AssertionError("Could not inspect index " + index, exception);
        }
    }
}
