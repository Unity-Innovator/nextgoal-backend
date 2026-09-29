# Backend response cache

Uses the existing Spring Redis connection. No new service or dependency is needed.
`BACKEND_CACHE_ENABLED` defaults to `true`; `ASSISTANT_CACHE_TTL` defaults to `PT2M`.
Set enabled to false or the AI TTL to `PT0S` to bypass caching.

- RAG replies are scoped by authenticated owner, exact trimmed question, models,
  similarity threshold, and the committed document ID set. Uploads/deletes change
  this revision across instances. The revision query includes all documents,
  including those beyond the first 100. Documents are immutable after upload;
  future in-place edits must add an update revision to this key.
- Public app-guide replies are shared: they contain only the bundled guide.
  Keys include model, question and retrieved passages.
- Signed media URLs are reused for five minutes within their one-hour validity.
  Keys include storage server, bucket and object path. Deletion evicts the entry.
  Existing authorization checks remain in the calling endpoints.
- SHA-256 keys omit raw user IDs/questions. Values contain replies/source excerpts
  or signed URLs; Redis must remain private and authenticated.
- Redis errors fall back to normal processing with a 30-second retry cooldown.
  Provider failures are not cached. Matching concurrent requests share work
  within a process; different instances can both compute a cold key.
- Authentication, input validation and rate limiting still run before AI lookup.

Cache entries expire automatically. This accelerates repeated AI requests and
media URL signing, not the first file transfer. Uploaded objects are not
 deduplicated because they have independent ownership and deletion lifecycles.

Run focused checks with `./mvnw -Dtest=BackendCacheTests,AssistantTests,RagTests,AssistantDiagnosticsTests,SupabaseStorageServiceTest test`.
