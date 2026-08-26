package com.example.genz.community;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommunityMigrationCompatibilityTest {

    @Test
    void v2CompletesPartialLegacyPostTablesBeforeAddingConstraints() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:community_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        )) {
            createPartialLegacySchema(connection);
            executeCompatibilityPreamble(connection);

            Map<String, List<String>> expectedColumns = Map.of(
                    "users", List.of("role"),
                    "posts", List.of(
                            "goal_id", "shared_post_id", "post_type", "content", "visibility",
                            "status", "is_pinned", "like_count", "comment_count", "share_count",
                            "created_at", "updated_at", "deleted_at"
                    ),
                    "post_comments", List.of(
                            "parent_comment_id", "content", "status", "like_count", "reply_count",
                            "created_at", "updated_at", "deleted_at"
                    ),
                    "post_likes", List.of("created_at"),
                    "post_reports", List.of(
                            "details", "status", "reviewed_by", "reviewed_at",
                            "moderation_note", "created_at"
                    )
            );

            expectedColumns.forEach((table, columns) -> columns.forEach(column ->
                    assertTrue(columnExists(connection, table, column),
                            () -> table + "." + column + " should be created by V2")
            ));

            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery(
                         "SELECT share_count, shared_post_id FROM posts WHERE post_id = " +
                                 "'00000000-0000-0000-0000-000000000010'"
                 )) {
                assertTrue(result.next());
                assertEquals(0, result.getInt("share_count"));
                assertNull(result.getObject("shared_post_id"));
            }
        }
    }

    private void createPartialLegacySchema(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE users (user_id UUID PRIMARY KEY)");
            statement.execute("CREATE TABLE goals (goal_id UUID PRIMARY KEY)");
            statement.execute("CREATE TABLE posts (post_id UUID PRIMARY KEY, user_id UUID NOT NULL)");
            statement.execute("""
                    CREATE TABLE post_comments (
                        comment_id UUID PRIMARY KEY,
                        post_id UUID NOT NULL,
                        user_id UUID NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE post_likes (
                        like_id UUID PRIMARY KEY,
                        post_id UUID NOT NULL,
                        user_id UUID NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE post_reports (
                        report_id UUID PRIMARY KEY,
                        post_id UUID NOT NULL,
                        reported_by UUID NOT NULL,
                        reason VARCHAR(30) NOT NULL
                    )
                    """);

            statement.execute("""
                    INSERT INTO users (user_id)
                    VALUES ('00000000-0000-0000-0000-000000000001')
                    """);
            statement.execute("""
                    INSERT INTO posts (post_id, user_id)
                    VALUES (
                        '00000000-0000-0000-0000-000000000010',
                        '00000000-0000-0000-0000-000000000001'
                    )
                    """);
        }
    }

    private void executeCompatibilityPreamble(Connection connection) throws Exception {
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/V2__community_constraints_and_indexes.sql"
        )) {
            assertNotNull(stream, "V2 migration resource should exist");
            String migration = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            int firstConstraintBlock = migration.indexOf("DO $$");
            assertTrue(firstConstraintBlock > 0, "V2 should contain its constraint block");

            String preamble = migration.substring(0, firstConstraintBlock);
            try (Statement statement = connection.createStatement()) {
                for (String sql : preamble.split(";")) {
                    if (!sql.isBlank()) {
                        statement.execute(sql);
                    }
                }
            }
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
