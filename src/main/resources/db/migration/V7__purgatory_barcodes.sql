CREATE TABLE purgatory_barcodes
(
    "purgatory_id" bigint            NOT NULL REFERENCES purgatory ("id") ON DELETE CASCADE,
    "source_name"  character varying NOT NULL REFERENCES sources ("name") ON DELETE CASCADE,
    "value"        character varying NOT NULL,
    "type"         character varying NOT NULL,

    CONSTRAINT pk_purgatory_barcodes PRIMARY KEY ("purgatory_id", "source_name", "value", "type")
);

CREATE INDEX idx_purgatory_barcodes_value ON purgatory_barcodes ("value");
