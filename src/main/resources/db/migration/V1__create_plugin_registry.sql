CREATE TABLE plugins (
    id          VARCHAR(255) PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT
);

CREATE TABLE plugin_versions (
    plugin_id VARCHAR(255) NOT NULL,
    version   VARCHAR(100) NOT NULL,
    manifest  JSONB NOT NULL,
    CONSTRAINT pk_plugin_versions PRIMARY KEY (plugin_id, version),
    CONSTRAINT fk_plugin_versions_plugin
        FOREIGN KEY (plugin_id) REFERENCES plugins (id) ON DELETE CASCADE
);

CREATE INDEX idx_plugin_versions_plugin_id ON plugin_versions (plugin_id);

CREATE FUNCTION reject_plugin_version_update()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'Published plugin versions are immutable';
END;
$$;

CREATE TRIGGER plugin_versions_are_immutable
BEFORE UPDATE ON plugin_versions
FOR EACH ROW
EXECUTE FUNCTION reject_plugin_version_update();
