ALTER TABLE books ADD COLUMN authors_text text GENERATED ALWAYS AS (authors::text) STORED;

CREATE INDEX idx_books_authors_text_trgm ON books USING gin (authors_text gin_trgm_ops);

DROP INDEX idx_books_title_trgm;
CREATE INDEX idx_books_title_trgm ON books USING gin (title gin_trgm_ops);
