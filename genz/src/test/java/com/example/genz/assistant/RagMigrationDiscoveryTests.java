package com.example.genz.assistant;

import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RagMigrationDiscoveryTests {
    @Test
    void productionAndRagMigrationsResolveTogether() {
        // Discovery only: PostgreSQL SQL is not executed against H2.
        var flyway = Flyway.configure()
                .dataSource("jdbc:h2:mem:rag_migration_discovery", "sa", "")
                .locations("classpath:db/migration", "classpath:db/rag")
                .load();
        var migrations = assertDoesNotThrow(() -> flyway.info().all());
        var scripts = Arrays.stream(migrations).map(m -> m.getScript()).toList();
        assertTrue(scripts.contains("V20__drop_legacy_otps_table.sql"));
        assertTrue(scripts.contains("V23__assistant_vector_knowledge.sql"));
        assertTrue(scripts.contains("V24__assistant_embedding_model.sql"));
        assertTrue(scripts.contains("V25__allow_shared_post_type.sql"));
    }
}
