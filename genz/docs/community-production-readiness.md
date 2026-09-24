# Community production readiness

## Database migrations

Production uses Flyway and Hibernate schema validation. The default local profile
keeps Flyway disabled and `ddl-auto=update` for the current development workflow.

This repository is adopting Flyway after an existing Hibernate-managed schema:

- `V1__baseline_existing_schema.sql` represents the legacy schema.
- `V2__community_constraints_and_indexes.sql` hardens community tables.
- Production uses `baseline-on-migrate=true` with baseline version `1`.
- Every later database change must use a new immutable migration. Never edit an
  already deployed migration.

Before the first production rollout:

1. Back up the PostgreSQL database.
2. Check for duplicate pending friend pairs, likes, reactions, and reports.
3. Check for negative counters and self-referencing relationships.
4. Repair invalid rows before Flyway runs; the migration deliberately fails when
   legacy data violates a new constraint.
5. Start with the `prod` Spring profile and verify Flyway and Hibernate validation
   complete before admitting traffic.

The Flyway baseline covers incremental adoption for an existing database. A fully
empty production database still requires the legacy core modules (`users`, goals,
wallets, learning, and rewards) to be provisioned until those modules receive
their own baseline migrations.

## Redis boundary

PostgreSQL remains authoritative for friendships, posts, comments, reports,
notifications, conversations, messages, counters, and read state.

Redis can be added later for derived or ephemeral state only:

- Implement `CommunityRateLimitStore` with atomic `INCR` plus expiry for global
  rate limits across application instances.
- Store WebSocket presence with short TTL heartbeats; never treat presence as a
  durable user state.
- Cache feed pages briefly and invalidate or expire them after writes. Always fall
  back to PostgreSQL and never persist posts only in Redis.

The current `InMemoryCommunityRateLimitStore` is safe for a single instance. It is
intentionally behind an interface so a Redis implementation does not change the
community services.

## Deployment checks

- Run `mvn test` before deployment.
- Run Flyway `validate` against the target database.
- Confirm the application uses `ddl-auto=validate` in production.
- Monitor HTTP 409/429 rates, database lock timeouts, and moderation queue age.
- Test backup restoration and migration rollback procedures in staging.
