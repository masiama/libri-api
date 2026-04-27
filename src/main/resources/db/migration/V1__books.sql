CREATE TABLE sources
(
    "name"     character varying NOT NULL UNIQUE,
    "priority" smallint          NOT NULL DEFAULT 100,
    "enabled"  boolean           NOT NULL DEFAULT true,
    PRIMARY KEY ("name")
);

CREATE TABLE books
(
    "isbn"        character varying NOT NULL UNIQUE,
    "title"       character varying NOT NULL,
    "authors"     jsonb             NOT NULL,
    "url"         character varying NOT NULL,
    "source_name" character varying NOT NULL REFERENCES sources ("name") ON DELETE CASCADE,
    PRIMARY KEY ("isbn")
);

CREATE INDEX idx_books_url ON books ("url");
