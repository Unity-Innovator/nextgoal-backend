CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE assistant_documents (
    id UUID PRIMARY KEY,
    owner_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    file_type VARCHAR(10) NOT NULL,
    chunk_count INTEGER NOT NULL CHECK (chunk_count > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX assistant_documents_owner_idx ON assistant_documents(owner_id);

CREATE TABLE assistant_document_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES assistant_documents(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    embedding vector(1536) NOT NULL,
    page_number INTEGER,
    chunk_index INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(document_id, chunk_index)
);
-- Exact search deliberately filters by owner before ranking. Add approximate
-- indexes only after measuring recall with ownership filters on a larger corpus.
