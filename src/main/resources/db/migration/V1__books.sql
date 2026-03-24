CREATE TABLE "public"."sources"
(
    "name"     character varying NOT NULL UNIQUE,
    "priority" smallint          NOT NULL DEFAULT 100,
    "enabled"  boolean           NOT NULL DEFAULT true,
    PRIMARY KEY ("name")
);

CREATE TABLE "public"."books"
(
    "isbn"        character varying NOT NULL UNIQUE,
    "title"       character varying NOT NULL,
    "authors"     jsonb             NOT NULL,
    "url"         character varying NOT NULL,
    "source_name" character varying NOT NULL REFERENCES "public"."sources" ("name"),
    PRIMARY KEY ("isbn")
);

CREATE UNIQUE INDEX "books_isbn_key" ON "public"."books" ("isbn");
