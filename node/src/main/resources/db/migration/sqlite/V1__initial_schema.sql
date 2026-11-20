CREATE TABLE vc_tasks (
  name VARCHAR(255) PRIMARY KEY,
  max_memory BIGINT NOT NULL,
  min_memory BIGINT NOT NULL,
  start_port BIGINT NOT NULL,
  static_services BOOLEAN NOT NULL,
  min_online_services INTEGER NOT NULL,
  max_online_services INTEGER NOT NULL,
  maintenance BOOLEAN NOT NULL,
  copy_templates_to_static BOOLEAN NOT NULL,
  service_factory_name VARCHAR(255) NOT NULL,
  max_players INTEGER NOT NULL,
  software_name VARCHAR(255) NOT NULL,
  software_version VARCHAR(255) NOT NULL,
  software_build INTEGER NOT NULL,
  software_url TEXT NOT NULL,
  software_plugin_dir TEXT NOT NULL,
  software_type VARCHAR(32) NOT NULL,
  fallback BOOLEAN NOT NULL,
  auto_start BOOLEAN NOT NULL
);

CREATE TABLE vc_task_preferred_nodes (task_name VARCHAR(255) NOT NULL, position INTEGER NOT NULL, node_name VARCHAR(255) NOT NULL, PRIMARY KEY (task_name, position), FOREIGN KEY (task_name) REFERENCES vc_tasks(name) ON DELETE CASCADE);
CREATE TABLE vc_task_jvm_args (task_name VARCHAR(255) NOT NULL, position INTEGER NOT NULL, arg_value TEXT NOT NULL, PRIMARY KEY (task_name, position), FOREIGN KEY (task_name) REFERENCES vc_tasks(name) ON DELETE CASCADE);
CREATE TABLE vc_task_env_vars (task_name VARCHAR(255) NOT NULL, position INTEGER NOT NULL, arg_value TEXT NOT NULL, PRIMARY KEY (task_name, position), FOREIGN KEY (task_name) REFERENCES vc_tasks(name) ON DELETE CASCADE);
CREATE TABLE vc_task_attributes (task_name VARCHAR(255) NOT NULL, attribute VARCHAR(255) NOT NULL, attribute_value TEXT NOT NULL, PRIMARY KEY (task_name, attribute), FOREIGN KEY (task_name) REFERENCES vc_tasks(name) ON DELETE CASCADE);
CREATE TABLE vc_task_templates (task_name VARCHAR(255) NOT NULL, position INTEGER NOT NULL, template_id VARCHAR(255) NOT NULL, template_name VARCHAR(255) NOT NULL, weight INTEGER NOT NULL, storage_name VARCHAR(255) NOT NULL, node_name VARCHAR(255), version VARCHAR(255) NOT NULL, enabled BOOLEAN NOT NULL, PRIMARY KEY (task_name, position), FOREIGN KEY (task_name) REFERENCES vc_tasks(name) ON DELETE CASCADE);

CREATE TABLE vc_users (
  username VARCHAR(255) PRIMARY KEY,
  password_hash VARCHAR(255) NOT NULL,
  extra_data_json TEXT NOT NULL
);
CREATE TABLE vc_groups (name VARCHAR(255) PRIMARY KEY);
CREATE TABLE vc_user_groups (
  username VARCHAR(255) NOT NULL,
  group_name VARCHAR(255) NOT NULL,
  PRIMARY KEY (username, group_name),
  FOREIGN KEY (username) REFERENCES vc_users(username) ON DELETE CASCADE,
  FOREIGN KEY (group_name) REFERENCES vc_groups(name) ON DELETE CASCADE
);
CREATE TABLE vc_user_permissions (
  username VARCHAR(255) NOT NULL,
  permission VARCHAR(512) NOT NULL,
  PRIMARY KEY (username, permission),
  FOREIGN KEY (username) REFERENCES vc_users(username) ON DELETE CASCADE
);
CREATE TABLE vc_group_permissions (
  group_name VARCHAR(255) NOT NULL,
  permission VARCHAR(512) NOT NULL,
  PRIMARY KEY (group_name, permission),
  FOREIGN KEY (group_name) REFERENCES vc_groups(name) ON DELETE CASCADE
);

CREATE TABLE vc_virtual_configs (
  name VARCHAR(255) PRIMARY KEY,
  created_at BIGINT NOT NULL,
  last_updated_at BIGINT NOT NULL,
  config_json TEXT NOT NULL
);

CREATE TABLE vc_templates (
  id VARCHAR(255) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  weight INTEGER NOT NULL,
  storage_name VARCHAR(255) NOT NULL,
  node_name VARCHAR(255),
  version VARCHAR(255) NOT NULL,
  enabled BOOLEAN NOT NULL
);

CREATE TABLE vc_template_job_records (
  job_id VARCHAR(255) PRIMARY KEY,
  type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  phase VARCHAR(64) NOT NULL,
  progress_phase VARCHAR(64) NOT NULL,
  source_json TEXT NOT NULL,
  destination_json TEXT NOT NULL,
  conflict_mode VARCHAR(32) NOT NULL,
  extra_files_mode VARCHAR(32) NOT NULL,
  total_files INTEGER NOT NULL,
  files_processed INTEGER NOT NULL,
  bytes_processed BIGINT NOT NULL,
  total_bytes BIGINT NOT NULL,
  current_file TEXT NOT NULL,
  files_copied INTEGER NOT NULL,
  files_skipped INTEGER NOT NULL,
  files_overwritten INTEGER NOT NULL,
  files_purged INTEGER NOT NULL,
  started_at BIGINT NOT NULL,
  completed_at BIGINT NOT NULL,
  failure_reason TEXT,
  worker_token VARCHAR(255) NOT NULL,
  source_inventory_json TEXT NOT NULL,
  source_directory_inventory_json TEXT NOT NULL,
  destination_existed BOOLEAN NOT NULL,
  initial_destination_inventory_json TEXT NOT NULL,
  expected_destination_inventory_json TEXT NOT NULL,
  completed_paths_json TEXT NOT NULL,
  purged_paths_json TEXT NOT NULL,
  in_flight_path TEXT,
  in_flight_purge_path TEXT,
  worker_node_name VARCHAR(255),
  worker_lease_until BIGINT NOT NULL,
  cancel_requested BOOLEAN NOT NULL,
  lock_keys_json TEXT NOT NULL
);

CREATE TABLE vc_template_job_locks (
  lock_key VARCHAR(255) PRIMARY KEY,
  job_id VARCHAR(255) NOT NULL,
  locked_at BIGINT NOT NULL,
  description TEXT NOT NULL,
  lease_until BIGINT NOT NULL
);

CREATE TABLE vc_rollouts (
  rollout_id VARCHAR(255) PRIMARY KEY,
  task_name VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL,
  strategy VARCHAR(32) NOT NULL,
  batch_size INTEGER NOT NULL,
  drain_player_threshold INTEGER,
  drain_timeout_seconds BIGINT,
  drain_timeout_nanos INTEGER,
  readiness_timeout_seconds BIGINT NOT NULL,
  readiness_timeout_nanos INTEGER NOT NULL,
  bypass_max_service_count BOOLEAN NOT NULL,
  fallback_remaining_players BOOLEAN NOT NULL,
  total_services_to_replace INTEGER NOT NULL,
  services_started INTEGER NOT NULL,
  services_ready INTEGER NOT NULL,
  services_stopped INTEGER NOT NULL,
  pending_stop_service_names_json TEXT NOT NULL,
  new_service_names_json TEXT NOT NULL,
  started_at_seconds BIGINT,
  started_at_nanos INTEGER,
  completed_at_seconds BIGINT,
  completed_at_nanos INTEGER,
  failure_reason TEXT NOT NULL,
  current_batch_number INTEGER NOT NULL,
  total_batches INTEGER NOT NULL
);

CREATE TABLE vc_node_drains (
  node_name VARCHAR(255) PRIMARY KEY,
  strategy VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  started_at_seconds BIGINT,
  started_at_nanos INTEGER,
  deadline_seconds BIGINT,
  deadline_nanos INTEGER,
  completed_at_seconds BIGINT,
  completed_at_nanos INTEGER,
  player_threshold INTEGER NOT NULL,
  services_total INTEGER NOT NULL,
  services_stopped INTEGER NOT NULL,
  services_remaining INTEGER NOT NULL,
  players_remaining INTEGER NOT NULL,
  failure_reason TEXT,
  drain_reason TEXT
);

CREATE TABLE vc_node_snapshots (
  node_name VARCHAR(255) PRIMARY KEY,
  node_uuid VARCHAR(36) NOT NULL,
  state VARCHAR(32) NOT NULL,
  players_on_node BIGINT NOT NULL,
  timestamp BIGINT NOT NULL,
  startup_timestamp BIGINT NOT NULL,
  system_json TEXT NOT NULL,
  services_json TEXT NOT NULL,
  attributes_json TEXT NOT NULL
);

CREATE TABLE vc_offline_players (
  uuid VARCHAR(36) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  first_seen BIGINT NOT NULL,
  last_seen BIGINT NOT NULL
);

CREATE TABLE vc_cluster_tls_records (
  record_key VARCHAR(255) PRIMARY KEY,
  ca_certificate_pem TEXT NOT NULL,
  encrypted_private_key_json TEXT NOT NULL
);

CREATE TABLE vc_chronyx_task_guards (task_name VARCHAR(255) PRIMARY KEY, touched_at BIGINT NOT NULL);

CREATE TABLE vc_chronyx_leases (
  task_name VARCHAR(255) NOT NULL,
  host_id VARCHAR(255) NOT NULL,
  token VARCHAR(255) NOT NULL,
  acquired_at BIGINT NOT NULL,
  expires_at BIGINT NOT NULL,
  PRIMARY KEY (task_name, token)
);
