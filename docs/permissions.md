# Fine-grained permissions

Permissions can be assigned to a user or group as a single token using the existing auth permission commands. For example:

```text
auth user Moderator1 add permission services.restart@task=SMP|CityBuild
auth group Moderators add permission cluster.*@node=Node-1
```

Append resource qualifiers after `@`:

```text
<permission>@<scope>=<value>[,<scope>=<value>...]
```

Examples:

- `services.restart@task=SMP` grants service restart permission for services whose task is `SMP`.
- `services.restart@task=SMP|CityBuild` grants service restart permission for either task.
- `tasks.prepareServiceOnTask@task=SMP` grants service preparation for task `SMP`.
- `cluster.drain@node=Node-1` grants drain access for `Node-1`.
- `cluster.*@node=Node-1` grants cluster permissions that have a matching node target on `Node-1`.
- `modules.restart@module=WorldEdit,node=Node-1` limits restarting that module to that node.
- `players.kick@player=550e8400-e29b-41d4-a716-446655440000` limits the player action to that player UUID.
- `rollout.cancel@rollout=<rollout-id>` limits cancellation to one rollout.
- `virtualconfig.update@virtualconfig=motd` limits changes to one virtual config.
- `templates.delete@template=Lobby,node=Node-1` limits template operations to that template and node.
- `services.restart@task=SMP,node=Node-1` requires both resource qualifiers to match; `|` separates alternatives within one scope.
- `services.restart@task=*` matches any task for that operation.

A permission without `@` keeps its existing global behavior. User and group grants are combined as allow rules; there are no explicit deny rules. When a request targets multiple resources, every targeted resource must match the grant. API resource values come from the RPC's declared or inferred protobuf request fields. Built-in command resource values are resolved for cluster nodes, modules, players, rollouts, services, tasks, templates, and virtual configs. Collection commands without a target require an unscoped permission. Module commands now use `modules.*` permissions; the console retains access.
