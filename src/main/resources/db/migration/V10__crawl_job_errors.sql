CREATE TABLE crawl_job_errors
(
    id           bigserial PRIMARY KEY,
    crawl_job_id bigint      NOT NULL REFERENCES crawl_jobs (id) ON DELETE CASCADE,
    message      text        NOT NULL,
    url          text,
    occurred_at  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_crawl_job_errors_job_id ON crawl_job_errors (crawl_job_id);
