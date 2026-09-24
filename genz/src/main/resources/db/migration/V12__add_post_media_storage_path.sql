ALTER TABLE post_media
    ADD COLUMN IF NOT EXISTS storage_path TEXT;
