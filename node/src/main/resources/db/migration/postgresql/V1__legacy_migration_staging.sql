CREATE TABLE vc_legacy_records (
    source_collection VARCHAR(255) NOT NULL,
    record_key VARCHAR(255) NOT NULL,
    payload_json TEXT NOT NULL,
    source_id VARCHAR(255),
    migrated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (source_collection, record_key)
);

CREATE TABLE vc_migration_manifest (
    source_collection VARCHAR(255) NOT NULL PRIMARY KEY,
    source_count BIGINT NOT NULL,
    migrated_count BIGINT NOT NULL,
    transformation_version VARCHAR(100) NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
