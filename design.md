### Rollout System Architecture Design & Protobuf Specifications

Here is the proposed architectural design, operational workflow, and `.proto` schema specifications for the **VulpesCloud Rollout System**.

---

### 1. Architecture & Core Concepts

#### 1.1 Service Lifecycle & States
Currently, services transition through `PREPARED -> STARTING -> RUNNING -> STOPPED`.
To support safe rollouts, routing segregation, and future health checking:
- **Service Drain State / Flag**: Services being replaced during a rollout are marked with a flag `draining: true` in service metadata / snapshot.
- **Service Generation / Rollout Tag**: Each service instance belongs to a rollout revision/generation (e.g., `active` vs `retiring` / `new`).
- **Readiness / Health abstraction**:
    - *Current*: A service is considered ready when its `state == ServiceStates.RUNNING`.
    - *Future*: In the Future Services will have the `health` key in the metadata. This will indicate whether the service is ready or not.

#### 1.2 Proxy & Routing Behavior (Passive / Graceful Rollouts)
When a rollout is initiated for a task (e.g. `Lobby`):
1. **Draining exclusion in Proxy / Connectors**:
    - In `PlayerChooseInitialServerEventListener`, `HubCommand`, and `ServiceAPI.getServicesByTask(...)`, proxies filter out services flagged as `DRAINING` or give priority to non-draining / newly deployed services.
    - New player joins and hub transfers are directed **only** to the new running services.
2. **Graceful Eviction / Drain Policy**:
    - **Passive Drain**: Let existing players finish their game or leave naturally.
    - **Threshold-based Drain**: When player count on an old service drops below `maxAllowedPlayers` (e.g. $\le 2$), or when `drainTimeout` elapses, the cloud gracefully transfers remaining players to a new service (or kicks them to fallback) and stops the old service.
    - **Force / Immediate Drain**: Stop immediately without waiting for players to leave.

#### 1.3 Capacity & Surge Constraints (`maxOnlineServices`)
- By default, the orchestrator calculates `maxSurge` (e.g., $+1$ or $+N$ services above current count) while respecting `Task.maxOnlineServices`.
- If starting new instances would violate `task.maxOnlineServices`, the rollout will:
    - Either perform a **replace-first** (stop 1 old $\rightarrow$ start 1 new) if `allowSurge == false`, OR
    - Temporarily allow surge beyond `maxOnlineServices` if `bypassMaxServiceCount == true` (or `--force`).

---

### 2. Rollout Strategies & Configuration

| Strategy | Description |
| :--- | :--- |
| `ROLLING_REPLACE` | Starts one Service, and waits for it to be available. Contunue until all old services are replaced. (Configurable how many services get replaced at once) |
| `PASSIVE_DRAIN` | Starts new services, marks old services as draining (proxy diverts incoming players away), waits for old service player count $\le$ threshold or timeout, then stops old services. |
| `IMMEDIATE` | Starts all target replacement services simultaneously and immediately shuts down old services. Does not wait for new Services to be RUNNING. And ignores maxOnlineServices|

---

### 3. Protobuf Specifications

Below are the `.proto` service and message definitions to be placed in `vulpescloud/rollout/v1/rollout.proto`.

```protobuf
syntax = "proto3";

package vulpescloud.rollout.v1;

import "google/protobuf/timestamp.proto";
import "google/protobuf/duration.proto";

option java_multiple_files = true;
option java_package = "de.vulpescloud.proto.rollout.v1";

enum RolloutStrategy {
  ROLLOUT_STRATEGY_UNSPECIFIED = 0;
  ROLLOUT_STRATEGY_ROLLING_REPLACE = 1;
  ROLLOUT_STRATEGY_PASSIVE_DRAIN = 2;
  ROLLOUT_STRATEGY_IMMEDIATE = 3;
}

enum RolloutStatus {
  ROLLOUT_STATUS_UNSPECIFIED = 0;
  ROLLOUT_STATUS_PENDING = 1;
  ROLLOUT_STATUS_IN_PROGRESS = 2;
  ROLLOUT_STATUS_DRAINING = 3;
  ROLLOUT_STATUS_COMPLETED = 4;
  ROLLOUT_STATUS_FAILED = 5;
  ROLLOUT_STATUS_CANCELLED = 6;
}

message RolloutOptions {
  RolloutStrategy strategy = 1;

  // Number of new services to start per batch (minimum 1, default 1)
  int32 batch_size = 2;

  // Maximum allowed players on an old service before forcing shutdown during drain (default: 0)
  // Only Applicable to ROLLOUT_STRATEGY_PASSIVE_DRAIN
  optional int32 drain_player_threshold = 3;

  // Timeout to wait for a service to drain players before forcing shutdown (0 = indefinite)
  // Only Applicable to ROLLOUT_STRATEGY_PASSIVE_DRAIN
  optional google.protobuf.Duration drain_timeout = 4;

  // Timeout to wait for a new service to reach RUNNING / Healthy state
  google.protobuf.Duration readiness_timeout = 5;

  // If true, ignores Task.maxOnlineServices constraint during rollout surge
  bool bypass_max_service_count = 6;

  // If true, automatically transfers remaining players to new services when drain_timeout expires
  bool fallback_remaining_players = 7;
}

message RolloutProgress {
  string rollout_id = 1;
  string task_name = 2;
  RolloutStatus status = 3;
  RolloutOptions options = 4;

  int32 total_services_to_replace = 5;
  int32 services_started = 6;
  int32 services_ready = 7;
  int32 services_stopped = 8;

  repeated string pending_stop_service_names = 9;
  repeated string new_service_names = 10;

  google.protobuf.Timestamp started_at = 11;
  google.protobuf.Timestamp completed_at = 12;
  string failure_reason = 13;
}

// gRPC Request & Response messages

message StartRolloutRequest {
  string task_name = 1;
  RolloutOptions options = 2;
}

message StartRolloutResponse {
  RolloutProgress rollout = 1;
}

message GetRolloutStatusRequest {
  string rollout_id = 1;
  string task_name = 2; // Optional filter if rollout_id is empty
}

message GetRolloutStatusResponse {
  RolloutProgress rollout = 1;
}

message ListActiveRolloutsRequest {}

message ListActiveRolloutsResponse {
  repeated RolloutProgress rollouts = 1;
}

message CancelRolloutRequest {
  string rollout_id = 1;
}

message CancelRolloutResponse {
  bool success = 1;
  string message = 2;
}

message StreamRolloutProgressRequest {
  string rollout_id = 1;
}

// Rollout API Service Definition for CLI, Dashboard, and Node orchestration
service RolloutAPIService {
  rpc StartRollout (StartRolloutRequest) returns (StartRolloutResponse);
  rpc GetRolloutStatus (GetRolloutStatusRequest) returns (GetRolloutStatusResponse);
  rpc ListActiveRollouts (ListActiveRolloutsRequest) returns (ListActiveRolloutsResponse);
  rpc CancelRollout (CancelRolloutRequest) returns (CancelRolloutResponse);
  rpc StreamRolloutProgress (StreamRolloutProgressRequest) returns (stream RolloutProgress);
}
```

---

### 4. CLI Command Design

Command syntax in the Node CLI and Proxy CLI:

```text
rollout restart <task> [options]
```

#### Flags / Arguments:
- `--strategy <rolling|passive|immediate>` (Default: `rolling`)
- `--batch <count>` / `-b <count>`: Number of instances to roll concurrently (Default: `1`).
- `--drain-threshold <players>`: Allowed remaining players before stopping old instance (Default: `0`). //Only applies to passive
- `--drain-timeout <seconds>` / `-t <seconds>`: Maximum wait time for drain (e.g., `60s`, `5m`). //Only applies to passive
- `--readiness-timeout <seconds>`: Max wait for new instance to report `RUNNING`/healthy (Default: `60s`).
- `--bypass-max-services` / `--force`: Allow temporary surge above `task.maxOnlineServices`.
- `--no-transfer`: Do not automatically transfer remaining players to the new service upon timeout.

#### Additional CLI commands:
- `rollout status <task|rolloutId>`: Shows live progress of an active rollout.
- `rollout cancel <rolloutId>`: Cancels an ongoing rollout.
- `rollout list`: Lists all ongoing rollouts across the cluster.

---

### 5. Step-by-Step Rollout Execution Flow

```text
[User / Dashboard triggers Rollout]
               │
               ▼
   Validate Task & Node capacity
   (Check maxOnlineServices / bypass flag)
               │
               ▼
   [Batch Loop: for each batch of old services]
   1. Prepare & Start new service(s) (batch_size >= 1)
   2. Poll / Stream state until state == RUNNING (and Health Check == OK)
   3. Mark target old service(s) as DRAINING:
      - Proxy updates routing table -> routes new player logins only to new service(s)
   4. Drain wait loop:
      - Wait until (player_count <= drain_player_threshold) OR (time >= drain_timeout)
   5. If players remaining & fallback_remaining_players == true:
      - Send connect request to new service for remaining players
   6. Stop & unregister old service(s)
               │
               ▼
   [Mark Rollout as COMPLETED]
```

---

### 6. Questions & Clarifications

To tailor the rollout system to your exact vision, please let me know:
1. **Default Rollout Mode**: Would you like `passive` (wait for players with timeout) or `rolling` (step-by-step immediate replacement) to be the default strategy when no flags are supplied?
    > Always rolling
2. **Player Migration on Timeout**: When the `drain_timeout` expires on a draining service, should remaining players automatically be transferred/redirected to the newly spawned service(s), or kicked to fallback/hub?
    > Configurable in VirtualConfig for defaults
    > Configurable via command flags
3. **Rollout Cancellation Behavior**: If an admin cancels a rollout halfway through:
    - Should it stop where it is (leaving both old and new services running)?
    - Or roll back by terminating the newly started services and un-marking old ones as draining?

    > The way vulpescloud is designed does not really allow us to go back to the old state.
    > But the best we can do is have it configurable that we do the following:
    > Terminate newly started Services
    > leave remaining old services alone and remove draining flag
4. **Task Configuration Defaults**: Would you like rollout defaults (such as default timeout, player threshold, and strategy) to be configurable per-Task in the database/JSON model as well?
   > Basicly everything should be configurable by default in a global VirtualConfig. But the option should be there to configure/override everything per task/per command (eg. using command flags and Task attributes <rollout.<option>: <value>>)
   > This means Flags > Task > Global
   > This path is being checked for every field. So that the user can for example set the default batch count for a task higher whilst everything else is the global default

5. **Flags**:
    > What is meant with flags here will be set in the StartRolloutRequest

### 7. Rollout Data Storage

Rollouts are stored in the `vc_rollouts` table/collection. Each task that is part of a rollout carries an attribute indicating that a rollout is active on it. Each service in a rollout also carries the rollout ID in its metadata.

Because nodes can restart unexpectedly, state can drift out of sync between `vc_rollouts` and the tasks/services that reference a rollout. A periodic reconciliation job (the "Rollout task") checks for two failure modes:

1. **Orphaned references** — a service or task carries a rollout ID that no longer exists in `vc_rollouts`. This means the rollout was deleted (or never fully written) but the reference survived a restart. → **Clear the rollout attribute/metadata on that service or task**, returning it to a non-rollout state, and log a warning so it's visible if this happens often (it may point to a race condition elsewhere, e.g. deleting a rollout before all references are cleaned up).
2. **Empty rollouts** — a rollout exists in `vc_rollouts` but no service or task references it anymore. → **Delete the rollout record.**

A few things worth pinning down further:

- **Ordering/race safety**: make sure step 1 and step 2 can't race with an in-progress rollout creation (e.g. a rollout row written before its first service/task is attached, and the reconciler running in between and deleting it as "empty"). A `created_at`/grace period or a `status` field (e.g. `pending` vs `active`) protects against this.
- **Idempotency**: since nodes restart mid-operation, both the "clear attribute" and "delete rollout" actions should be safe to run repeatedly without side effects.
- **Consistency scope**: decide whether the reconciler operates per-node (only fixing tasks/services on the node that just restarted) or globally (a periodic sweep across all nodes) — the phrasing above suggests global, which is probably right for catching orphans left by a node that restarted and never came back.

### 8. Rollout execution
Not fully decided on yet. But a proposal is the following:
The rollout will be done in a reconciliation way (like AutomaticServiceStarting). One Chronyx task running on only one node at the same time will do all the handling.
1. Check for queued rollouts
2. Run rollout step
3. Clear orphaned data

### 9. User notification
Every step will have its own events that are going to be triggered.
The important events will be listened on and logged in the Console of every node.
The separate Notify-Module will be responsible for Notifying players on the Network and other places like Discord.