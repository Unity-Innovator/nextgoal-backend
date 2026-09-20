package com.example.genz.schema;

import com.example.genz.challenge.enums.ChallengeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DailyChallengeTypeConstraintMigrationTest {

    @Test
    @DisplayName("V19 updates daily_challenges_challenge_type_check to allow all ChallengeType enum values and rejects invalid types")
    void v19UpdatesChallengeTypeCheckConstraint() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:challenge_type_constraint_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        )) {
            createLegacyDailyChallengesTable(connection);

            // 1. Verify that inserting DEPOSIT_AMOUNT fails under the legacy constraint
            SQLException ex = assertThrows(SQLException.class, () -> insertChallenge(connection, "DEPOSIT_AMOUNT"));
            assertTrue(ex.getMessage().contains("DAILY_CHALLENGES_CHALLENGE_TYPE_CHECK")
                    || ex.getMessage().toLowerCase().contains("check constraint"));

            // 2. Execute V19 twice to verify idempotency
            executeV19(connection);
            executeV19(connection);

            // 3. Verify all enum values can be inserted successfully
            for (ChallengeType type : ChallengeType.values()) {
                insertChallenge(connection, type.name());
            }

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM daily_challenges")) {
                assertTrue(rs.next());
                assertEquals(ChallengeType.values().length, rs.getInt(1));
            }

            // 4. Verify that an invalid challenge type is still rejected by the updated check constraint
            SQLException invalidEx = assertThrows(SQLException.class, () -> insertChallenge(connection, "INVALID_TYPE"));
            assertTrue(invalidEx.getMessage().contains("DAILY_CHALLENGES_CHALLENGE_TYPE_CHECK")
                    || invalidEx.getMessage().toLowerCase().contains("check constraint"));
        }
    }

    private void createLegacyDailyChallengesTable(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE daily_challenges (
                        challenge_id UUID PRIMARY KEY,
                        title VARCHAR(200) NOT NULL,
                        description VARCHAR(500),
                        challenge_type VARCHAR(255) NOT NULL,
                        xp_reward INTEGER,
                        is_active BOOLEAN NOT NULL,
                        created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                        CONSTRAINT daily_challenges_challenge_type_check
                            CHECK (challenge_type IN ('GOAL_DEPOSIT', 'QUIZ', 'EXPENSE_LOG', 'TIP', 'MANUAL'))
                    )
                    """);
        }
    }

    private void executeV19(Connection connection) throws Exception {
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/V19__update_daily_challenges_challenge_type_check.sql"
        )) {
            assertNotNull(stream, "V19 migration resource should exist");
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

    private void insertChallenge(Connection connection, String challengeType) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(String.format("""
                    INSERT INTO daily_challenges (challenge_id, title, description, challenge_type, xp_reward, is_active, created_at)
                    VALUES ('%s', 'Test Title', 'Test Description', '%s', 25, TRUE, CURRENT_TIMESTAMP)
                    """, UUID.randomUUID(), challengeType));
        }
    }
}
