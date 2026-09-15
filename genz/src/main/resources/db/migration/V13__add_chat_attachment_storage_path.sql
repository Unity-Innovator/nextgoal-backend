ALTER TABLE chat_message_attachments
    ADD COLUMN IF NOT EXISTS storage_path TEXT;
