ALTER TABLE crawl_jobs
    ADD COLUMN last_heartbeat_at timestamptz;

ALTER TABLE crawl_jobs
    ALTER COLUMN started_at TYPE timestamptz USING started_at AT TIME ZONE 'UTC';

ALTER TABLE crawl_jobs
    ALTER COLUMN finished_at TYPE timestamptz USING finished_at AT TIME ZONE 'UTC';

ALTER TABLE purgatory
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC';
