# Document RAG assistant (V1)

The Flutter assistant now includes **My documents** for PDF/DOCX/TXT/MD upload,
listing, and deletion. The backend extracts text, chunks it, creates Gemini
embeddings, stores vectors in PostgreSQL/pgvector, retrieves up to five private
passages, and sends those passages to Gemini 3.5 Flash. Answers include retrieved
source IDs, names, excerpts, and PDF page numbers.

## Enable

1. Install pgvector on the existing PostgreSQL 17 server, or use the optional
   Compose override below. Back up an existing database before changing its image.
2. Provide GEMINI_API_KEY to the backend process. Never put it in Flutter.
3. Set SPRING_PROFILES_ACTIVE=prod,rag (rag must come last), keeping the existing
   datasource, JWT, Redis, and other production settings.
4. Restart the backend. The rag profile enables Flyway and adds db/rag alongside
   existing migrations. V20 creates the vector extension and assistant tables.
   The migration role needs extension-creation rights, or an administrator must
   first install vector in the public schema.
5. Sign in, open the assistant, choose My documents, upload a small text document,
   and ask a question that it answers.

Compose example (run from genz, supplying existing required environment settings):

```powershell
docker compose -f docker-compose.prod.yaml -f docker-compose.rag.yaml up -d --build
```

The override uses pgvector/pgvector:pg17 and passes the Gemini key into the backend.
It does not run automatically. No existing database was changed during development.

### GitHub Actions deployment

The Backend CI workflow applies both production and RAG Compose files on the
existing self-hosted Ubuntu deployment runner. Tests and image build must pass
before deployment. Pushes to main deploy automatically; alternatively, select
Actions → Backend CI → Run workflow, select main, and enable the deploy input.
The workflow change must be committed and pushed before Actions can use it.

Before the first RAG deployment, back up the existing PostgreSQL database: the
override changes its image to pgvector/pgvector:pg17 and enables RAG migrations.
Keep the existing nextgoal Compose project and database volume.

Set GEMINI_API_KEY in `/etc/default/nextgoal-env` on the Ubuntu runner host,
alongside the existing deployment settings. The workflow reads that file
explicitly; adding a key only to a checkout's .env or GitHub Secrets will not
pass it into this deployment. Do not commit the key. An optional
ASSISTANT_RAG_MODEL value in the same file overrides the configured default.

The workflow validates the merged configuration with `config --quiet` before
changing containers, so a missing Gemini key fails without printing the resolved
secrets. The RAG override sets SPRING_PROFILES_ACTIVE to prod,rag and updates the
database dependency as part of `up -d --build backend`.

After deployment, verify `docker exec nextgoal-backend printenv
SPRING_PROFILES_ACTIVE` reports prod,rag and check backend startup logs for
migration errors. Compose status alone does not confirm the Gemini API works;
upload a document and ask a question about it to verify the complete flow.

ASSISTANT_RAG_MODEL defaults to gemini-3.5-flash. The embedding model is deliberately
fixed to gemini-embedding-001 with 1536 dimensions. Changing it requires a
migration and re-embedding every document; never mix embedding models in one index.
assistant.rag.min-similarity defaults to 0.3; calibrate it on representative questions.

Without the rag profile, the original bundled-guide/Groq assistant remains active.
Once V20 has been applied, keep classpath:db/rag in Flyway's locations even if
disabling assistant.rag.enabled, so Flyway can still validate its migration history.
Bundled guides are not automatically sent to Gemini; upload guides you want to use.

## API

All routes require the existing bearer authentication. Ownership comes exclusively
from the authenticated principal; requests cannot supply another owner's identity.

| Method | Route | Body/result |
| --- | --- | --- |
| POST | /api/assistant/documents | multipart field file; 201 data={id,name,fileType,chunkCount} |
| GET | /api/assistant/documents | 200 data=[documents], newest 100 |
| DELETE | /api/assistant/documents/{id} | 204; 404 for missing or another owner's document |
| POST | /api/assistant/chat | JSON {"message":"What is the refund period?"} |

Chat returns the existing envelope with data.answer and data.sources.
Each source has id, title, excerpt, pageNumber (nullable). The mobile app displays
source IDs to connect inline citations to passages. Retrieved sources are evidence
candidates, not a guarantee of factual correctness. An empty knowledge base or no
match above the similarity threshold returns an insufficient-information answer.

Upload limits: 10 MB, 100 PDF pages, 200,000 extracted characters, and 100 chunks.
DOCX ZIP expansion and XML external entities are restricted. Scanned PDFs need
OCR elsewhere. DOCX has no reliable page numbering, so its pageNumber is null.
Chunks contain at most 2,400 characters with 200-character overlap, approximately
600 English tokens (not an exact or language-independent token count).
Uploads run synchronously, with two concurrent uploads per backend process.
Embedding requests use batches of 16 and finite, nonzero 1536-element validation.
All embeddings complete before one database transaction inserts metadata/chunks.
Extraction/provider failure leaves no partial indexed document.
Deleting metadata cascades to chunks; no original file bytes are retained.

Retrieval uses a materialized owner-filtered set before exact cosine ranking.
No approximate index is used in V1; measure recall and query time before adding one.
Chat retains the existing per-user, per-process ten-requests/minute limit.
Multi-instance limits, storage quotas, background indexing, OCR, tenant sharing,
hybrid retrieval, reranking, query rewriting, and server conversation memory
remain later phases. Questions are independent; visible chat history is local
and does not provide model memory. Existing local history is not account scoped.

## Verification

```powershell
.\mvnw.cmd "-Dtest=AssistantTests,RagTests,RagPostgresTests" test
```

Unit tests mock Gemini. RagPostgresTests runs only when RAG_TEST_DATABASE_URL is
set, with optional RAG_TEST_DATABASE_USER and RAG_TEST_DATABASE_PASSWORD.
Use a disposable PostgreSQL database with vector already installed in public.
The test creates/removes its own random schema and checks real cosine retrieval,
owner isolation, atomic rollback, and deletion cascade.

Frontend: flutter test --no-pub test/ai_service_test.dart test/ai_documents_service_test.dart

Live acceptance: upload a policy as user A; verify a supported answer and source
page; ask an unrelated question; sign in as user B and verify no access; delete
the policy as A and verify subsequent retrieval excludes it. No live Gemini call
is required for unit tests. Live API access and PostgreSQL integration must be
verified in the configured environment.

## Implementation choice and references

This extends the project's existing Spring RestClient integration instead of
introducing Spring AI and its auto-configuration. The extraction, provider, store,
and orchestration components remain separate so adapters can be replaced later.

- [Gemini 3.5 Flash](https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash)
- [Gemini embeddings](https://ai.google.dev/gemini-api/docs/embeddings)
- [pgvector](https://github.com/pgvector/pgvector)
- [PDFBox 3 loading](https://pdfbox.apache.org/3.0/migration.html)

## Switching from OpenAI embeddings

V21 labels existing vectors as text-embedding-3-small. Gemini retrieval excludes
those documents until you delete and re-upload them; their metadata remains visible.
New uploads use gemini-embedding-001 at 1536 dimensions with RETRIEVAL_DOCUMENT;
questions use RETRIEVAL_QUERY. Existing migration checksums remain unchanged.
The Gemini key must have access to gemini-3.5-flash and gemini-embedding-001.
The bundled-guide fallback still uses Groq when the rag profile is disabled.
