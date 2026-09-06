### VulpesCloud Rollout System — Step-by-Step Implementation Plan

Based on the architectural specifications in `design.md` and the existing VulpesCloud codebase, here is the detailed implementation plan:

---

### Phase 1: Domain Models, Proto Mapping & Configuration

1. **API Domain Models & Protobuf Mapping (`api` module)**
    - Create data classes and enums under `org.vulpesstudios.vulpescloud.api.rollout`:
        - `RolloutStrategy`: `ROLLING_REPLACE`, `PASSIVE_DRAIN`, `IMMEDIATE`.
        - `RolloutStatus`: `PENDING`, `IN_PROGRESS`, `DRAINING`, `COMPLETED`, `FAILED`, `CANCELLED`.
        - `RolloutOptions`: `strategy`, `batchSize`, `drainPlayerThreshold`, `drainTimeout`, `readinessTimeout`, `bypassMaxServiceCount`, `fallbackRemainingPlayers`.
        - `RolloutProgress`: `rolloutId`, `taskName`, `status`, `options`, counters (`totalServicesToReplace`, `servicesStarted`, `servicesReady`, `servicesStopped`), service name lists (`pendingStopServiceNames`, `newServiceNames`), timestamps (`startedAt`, `completedAt`), and `failureReason`.
    - Implement `toDefinition()` and `fromDefinition()` to convert between domain models and generated Protobuf classes (`build.buf.gen.vulpescloud.rollout.v1.*`).
    - Add metadata helper extensions on `Service` (e.g., `Service.isDraining(): Boolean`, `Service.rolloutId(): String?`).

2. **Hierarchical Configuration Resolution (`Flags > Task Attributes > VirtualConfig`)**
    - Register default rollout configuration keys in `VirtualConfig` (e.g. `rollout.default_strategy`, `rollout.default_batch_size`, `rollout.default_readiness_timeout`, `rollout.default_drain_timeout`, `rollout.default_drain_threshold`, `rollout.default_fallback_players`).
    - Implement a `RolloutConfigResolver` that merges settings in order of precedence:
        1. Request parameters / CLI command flags (highest priority)
        2. Task attributes (e.g., `task.attributes["rollout.<option>"]`)
        3. Global `VirtualConfig` defaults (fallback)

---

### Phase 2: Database Storage & State Management

1. **Rollout Storage Repository (`vc_rollouts`)**
    - Register the `vc_rollouts` collection / table across database providers (`MongoDBDatabaseProvider`, `SQLiteDatabaseProvider`, `MariaDBDatabaseProvider`).
    - Create `RolloutStorage` to handle CRUD operations on rollouts with thread-safe / atomic updates.
2. **Metadata & State Attachment**
    - Tag newly spawned services with `rollout_id` and rollout generation in their `metadata`.
    - Tag retiring services with `draining: "true"` in their `metadata`.
    - Tag active tasks with the `rollout_id` attribute.

---

### Phase 3: Proxy & Connector Draining Integration

1. **Routing & Server Filtering (`connector` & `bridge` modules)**
    - Update `PlayerChooseInitialServerEventListener` in the Velocity connector to exclude services where `service.isDraining()` is true.
    - Update `HubCommand` to ignore draining fallback instances when finding target lobby/hub servers.
    - Update `KickedFromServerEvent` to prevent redirecting disconnected players to draining services.

---

### Phase 4: Rollout Engine & Chronyx Coordinator

1. **Chronyx Reconciliation Task (`rollout-reconciler`)**
    - Register a cluster-wide Chronyx scheduled task in `ChronyxCoordinator` running exclusively on the active coordinator node (e.g., every 2–3 seconds).
2. **Execution State Machine per Strategy**
    - **Capacity & Surge Validation**:
        - Check `task.maxOnlineServices` against current + new batch count. If surge exceeds limit without `bypassMaxServiceCount`, queue or execute replace-first.
    - **`ROLLING_REPLACE` Flow**:
        - Step 1: Start `batch_size` new services and wait until state reaches `RUNNING` (and health check ok) within `readiness_timeout`.
        - Step 2: Mark corresponding batch of old services as `draining: true`.
        - Step 3: Stop old services and remove their registration.
        - Step 4: Repeat until all target old services are replaced, then mark `COMPLETED`.
    - **`PASSIVE_DRAIN` Flow**:
        - Step 1: Start replacement services and await `RUNNING`.
        - Step 2: Mark target old services as `draining: true` (diverts new joins away).
        - Step 3: Drain wait loop — monitor `playerCount`. If `playerCount <= drain_player_threshold` or `drain_timeout` expires:
            - If `fallback_remaining_players == true` and players remain, issue player transfer commands to newly ready services / fallback.
            - Stop old services.
        - Step 4: Mark `COMPLETED` once all old services are terminated.
    - **`IMMEDIATE` Flow**:
        - Step 1: Launch all replacement instances concurrently.
        - Step 2: Immediately stop all old instances without awaiting readiness.
        - Step 3: Mark `COMPLETED`.
3. **Rollout Cancellation Handling**
    - If cancelled via API/CLI:
        - Terminate newly started services associated with the rollout.
        - Remove `draining` flag from remaining old services.
        - Remove rollout attribute from task and mark rollout record as `CANCELLED`.
4. **Self-Healing & Reconciler Sweeps**
    - **Orphaned references**: Clear rollout metadata/attributes on tasks and services if their referenced rollout record no longer exists in `vc_rollouts`.
    - **Empty / Completed rollouts**: Clean up stale completed/cancelled rollout records past the retention/grace window.

---

### Phase 5: Cluster Events & Notification System

1. **Rollout Lifecycle Events**
    - Create cluster events: `RolloutStartedEvent`, `RolloutBatchProgressEvent`, `RolloutDrainingEvent`, `RolloutCompletedEvent`, `RolloutFailedEvent`, `RolloutCancelledEvent`.
    - Publish events over the cluster message bus when rollout state transitions occur.
2. **Node Console Logging & Notifications**
    - Register local event listeners on nodes to print formatted ANSI log messages during rollout stages.
    - Provide integration hooks for the Notify Module (Discord / In-game player notifications).

---

### Phase 6: gRPC Service & Bridge APIs   ← Current step

1. **`RolloutAPIService` gRPC Implementation (`node` module)**
    - Implement `RolloutAPIServiceGrpcKt.RolloutAPIServiceCoroutineImplBase`:
        - `startRollout`: Validates task, computes effective options via resolver, persists initial state in DB, returns `RolloutProgress`.
        - `getRolloutStatus`: Returns current progress by rollout ID or task name.
        - `listActiveRollouts`: Returns all active rollouts (`PENDING`, `IN_PROGRESS`, `DRAINING`).
        - `cancelRollout`: Triggers cancellation routine.
        - `streamRolloutProgress`: Server-streaming RPC emitting live progress updates.
2. **Bridge Client API (`bridge` module)**
    - Add `RolloutAPI`, `RolloutCoroutineAPI`, and `RolloutFutureAPI` interfaces and implementations.
    - Wire `RolloutAPI` into `BridgeAPI` and `LocalGrpcClient`.

---

### Phase 7: Node & Proxy CLI Commands

1. **Command Implementation (`RolloutCommand`) using Cloud Framework**
    - `rollout restart <task> [flags]`
        - Flags: `--strategy <rolling|passive|immediate>`, `--batch <count>` / `-b <count>`, `--drain-threshold <players>`, `--drain-timeout <seconds>` / `-t <seconds>`, `--readiness-timeout <seconds>`, `--bypass-max-services` / `--force`, `--no-transfer`.
    - `rollout status <task|rolloutId>`: Display live status/progress of an active rollout.
    - `rollout cancel <rolloutId>`: Cancel an ongoing rollout.
    - `rollout list`: Tabulate active and recent rollouts across the cluster.
2. **Suggestions & Permissions**
    - Provide auto-completion suggestions for task names and active rollout IDs.
    - Add permission checks (e.g. `rollout.restart`, `rollout.cancel`, `rollout.view`).

---

### Phase 8: Verification & Unit/Integration Testing

1. **Unit Tests**:
    - Option resolution hierarchy (`Flags > Task Attributes > VirtualConfig`).
    - Protobuf to Domain Model conversions.
2. **Rollout Engine & Strategy Tests**:
    - Batch replacement and capacity surge check.
    - Passive drain timeout and fallback player transfer logic.
    - Cancellation cleanup logic.
    - Reconciler orphan reference clearing.
3. **Proxy Routing Tests**:
    - Verify draining services are ignored during player connect and hub transfer events.