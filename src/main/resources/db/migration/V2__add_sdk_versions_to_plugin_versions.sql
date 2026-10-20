ALTER TABLE plugin_versions
    ADD COLUMN frontend_sdk_version VARCHAR(100)
        GENERATED ALWAYS AS (manifest #>> '{frontend,sdk-version}') STORED,
    ADD COLUMN backend_sdk_version VARCHAR(100)
        GENERATED ALWAYS AS (manifest #>> '{backend,sdk-version}') STORED;
