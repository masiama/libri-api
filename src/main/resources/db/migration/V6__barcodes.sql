CREATE TABLE barcodes
(
    "isbn"        character varying NOT NULL REFERENCES books ("isbn") ON DELETE CASCADE,
    "source_name" character varying NOT NULL REFERENCES sources ("name") ON DELETE CASCADE,
    "value"       character varying NOT NULL,
    "type"        character varying NOT NULL,

    CONSTRAINT pk_barcodes PRIMARY KEY ("isbn", "source_name", "value", "type")
);

CREATE INDEX idx_barcodes_value ON barcodes ("value");
