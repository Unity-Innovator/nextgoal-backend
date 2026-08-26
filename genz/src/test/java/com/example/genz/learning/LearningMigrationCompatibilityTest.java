package com.example.genz.learning;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningMigrationCompatibilityTest {

    @Test
    void v3UpgradesLegacyUserAnswersBeforeCreatingItsIndex() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:learning_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        )) {
            createPreQuizAttemptSchema(connection);

            executeV3(connection);
            executeV3(connection);

            assertTrue(columnExists(connection, "user_answers", "quiz_attempt_id"));
            assertTrue(columnExists(connection, "quiz_attempt_questions", "quiz_attempt_id"));
            assertTrue(columnExists(connection, "quiz_attempt_questions", "question_id"));
            assertTrue(indexExists(connection, "user_answers", "ix_user_answers_quiz_attempt_id"));
        }
    }

    private void createPreQuizAttemptSchema(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE users (user_id UUID PRIMARY KEY)");
            statement.execute("""
                    CREATE TABLE lessons (
                        lesson_id UUID PRIMARY KEY,
                        title VARCHAR(200) NOT NULL,
                        category VARCHAR(50) NOT NULL,
                        content TEXT,
                        difficulty VARCHAR(50) NOT NULL,
                        points_reward INT,
                        is_active BOOLEAN,
                        created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMP WITHOUT TIME ZONE
                    )
                    """);
            statement.execute("""
                    CREATE TABLE lesson_questions (
                        question_id UUID PRIMARY KEY,
                        lesson_id UUID NOT NULL REFERENCES lessons(lesson_id) ON DELETE CASCADE,
                        question TEXT NOT NULL,
                        option_a VARCHAR(500) NOT NULL,
                        option_b VARCHAR(500) NOT NULL,
                        option_c VARCHAR(500) NOT NULL,
                        option_d VARCHAR(500) NOT NULL,
                        correct_answer VARCHAR(10) NOT NULL,
                        order_index INT
                    )
                    """);
            statement.execute("""
                    CREATE TABLE user_lessons (
                        user_lesson_id UUID PRIMARY KEY,
                        user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                        lesson_id UUID NOT NULL REFERENCES lessons(lesson_id) ON DELETE CASCADE,
                        status VARCHAR(50) NOT NULL,
                        score INT,
                        started_at TIMESTAMP WITHOUT TIME ZONE,
                        completed_at TIMESTAMP WITHOUT TIME ZONE
                    )
                    """);
            statement.execute("""
                    CREATE TABLE user_answers (
                        user_answer_id UUID PRIMARY KEY,
                        user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                        question_id UUID NOT NULL REFERENCES lesson_questions(question_id) ON DELETE CASCADE,
                        selected_answer VARCHAR(50) NOT NULL,
                        is_correct BOOLEAN,
                        answered_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
                    )
                    """);
        }
    }

    private void executeV3(Connection connection) throws Exception {
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/V3__create_quiz_attempt_questions_table.sql"
        )) {
            assertNotNull(stream, "V3 migration resource should exist");
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

    private boolean columnExists(Connection connection, String table, String column) throws Exception {
        try (ResultSet result = connection.getMetaData().getColumns(
                null,
                null,
                table.toUpperCase(),
                column.toUpperCase()
        )) {
            return result.next();
        }
    }

    private boolean indexExists(Connection connection, String table, String index) throws Exception {
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
        }
    }
}
