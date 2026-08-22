-- Flyway adoption baseline.
--
-- The application predates Flyway, so version 1 represents the schema that was
-- previously managed by Hibernate ddl-auto=update. Production enables
-- baseline-on-migrate at version 1 for an existing non-empty database. All new
-- production schema changes must be introduced in a later versioned migration.
SELECT 1;
