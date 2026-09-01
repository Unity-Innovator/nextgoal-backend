package com.example.genz.schema;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreshDatabaseBaselineMigrationTest {

    private static final String URL =
            "jdbc:h2:mem:fresh_baseline;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

    @Test
    void emptyDatabaseUsesExecutableBaselineInsteadOfMarkerMigration() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(URL, "sa", "")
                .locations("classpath:db/migration")
                .target("1")
                .load();

        flyway.migrate();

        assertNotNull(flyway.info().current());
        assertEquals("1", flyway.info().current().getVersion().toString());

        List<String> requiredCoreTables = List.of(
                "users",
                "goals",
                "goal_members",
                "group_wallets",
                "user_settings",
                "badges",
                "posts",
                "post_comments"
        );

        try (var connection = DriverManager.getConnection(URL, "sa", "")) {
            for (String table : requiredCoreTables) {
                try (ResultSet result = connection.getMetaData().getTables(
                        null,
                        null,
                        table.toUpperCase(),
                        new String[]{"TABLE"}
                )) {
                    assertTrue(result.next(), () -> table + " should be created by B1");
                }
            }
        }
    }
}
