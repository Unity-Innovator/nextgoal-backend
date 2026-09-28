-- Preserve legacy vectors, but never rank them against Gemini query vectors.
ALTER TABLE assistant_documents ADD COLUMN embedding_model VARCHAR(100) NOT NULL DEFAULT 'text-embedding-3-small';
