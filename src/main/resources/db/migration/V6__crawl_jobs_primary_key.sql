ALTER TABLE "public"."crawl_jobs"
    DROP CONSTRAINT crawl_jobs_pkey;
ALTER TABLE "public"."crawl_jobs"
    ADD PRIMARY KEY ("id");
