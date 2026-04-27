CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_books_title_trgm ON books USING gist ("title" gist_trgm_ops);
