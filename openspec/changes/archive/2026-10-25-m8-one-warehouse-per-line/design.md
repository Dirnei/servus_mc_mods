# Design

## Context

`terminal/TerminalWarehouses.reachable(level, pos)` walks the members of the data line at `pos` and collects the warehouses of every Warehouse Controller and Packing Station it finds, online ones only, sorted by controller position. `TerminalWarehouses.stock(list)` merges their requestable stock and caches it by index and reservation version. Every user of a warehouse on the line goes through `reachable`: `TerminalRequests` (requests, retries, lamp), `RemoteTransfers`, `StorageTerminalMenu` (stock sync, Storage tab data), `ProductionView`, `CpuBlockEntity` (order stock), `CraftingStationBlockEntity` (power payer). The Input row picks `nearestPackingStation` by straight distance among all Packing Stations of the line. `WarehouseSupplier` ships from the warehouse of its own node and does not look at the line.

PLAN.md leaves no open decision for this change; the explore session settled that the rule is enforced as a conflict state, not by refusing placement.

## Goals / Non-Goals

**Goals:**
- One place that answers "which warehouse does this line belong to": none, one, or conflict.
- Every block that used the list of warehouses uses the single answer and shows the conflict.

**Non-Goals:**
- Caching the answer across ticks. It is computed on demand, as `reachable` is today; the callers already throttle (lamp every `LAMP_TICKS`, retries every `RETRY_TICKS`, menus on sync).
- Any change to wireless sites, the site selector or `RemoteTransfers` beyond reading the single warehouse.

## Decisions

### `LineWarehouse` replaces the list
`TerminalWarehouses.line(level, pos)` returns a `LineWarehouse` result: `NONE`, `ONE(warehouse)` or `CONFLICT`. Members count whatever the warehouse's status, so an offline second warehouse still puts the line in conflict (otherwise powering it would suddenly break a working line). A rail network without a controller is no warehouse and is not counted. `ONE` with an offline warehouse acts as `NONE` for callers that need an online warehouse, through `LineWarehouse.online()` returning `Optional<WarehouseNetwork>`.

`TerminalWarehouses.online(level, pos)` is the shorthand most callers use. `reachable` and `TerminalRequests.first` are removed rather than kept as a one element list, so no caller keeps silently looping over "all" warehouses. `stock(list)` becomes `stock(warehouse)`; the merge path and the list keyed cache go, the cache is keyed by the warehouse.

Alternative considered: keep `reachable` and return an empty list on conflict. Smaller diff, but every caller then reports "offline" instead of the conflict, and the multi warehouse loops stay in the code.

### Pure classification
`terminal/LineClassification` (plain Java) takes the identities of the warehouses found among the members and returns none, one or conflict, comparing by identity. JUnit covers no members, one warehouse seen through several members, two warehouses.

### Conflict per caller
| Caller | On `CONFLICT` |
|---|---|
| `TerminalRequests.request` | refusal key `line_conflict` |
| `TerminalRequests.retry` | requests wait, nothing ends |
| `TerminalRequests.tick` lamp | dark |
| `StorageTerminalBlockEntity.storeInput` | `inputRefusal = "line_conflict"`, no move starts |
| `StorageTerminalMenu` stock sync | empty stock, message `line_conflict` |
| `StorageTerminalMenu` Storage tab data / `ProductionView` | conflict flag in the payload, the screen draws the alert line |
| `CpuBlockEntity.Host.stock` | empty stock, so the planner starts no step from the warehouse; the summary line names the conflict |
| `CraftingStationBlockEntity` payer | none, so it draws no power |
| `PackingStationBlockEntity.ship` | starts no data delivery, still returns orphans; status line `line_conflict` (its lamps stay as the warehouse sets them) |
| `WarehouseSupplier.supply` | 0 when the node's line is in conflict |

The Packing Station and the supplier look up the line of their own position; a Packing Station without a data line is never in conflict and keeps serving wireless transfers.

### Input row target
`nearestPackingStation` keeps its distance rule but only considers Packing Stations of the line's warehouse. `m8-wtp-ingress` replaces the rule with the warehouse's choice.

### APIs
No new NeoForge API. Data line members come from Foundation's `DataNetworks.networkAt` and `DataNetwork.dataMembers()`, as today. Lang keys via `ServusShuttlesLanguageProvider`.

## Risks / Trade-offs

- [Worlds with two warehouses on one line stop requesting after the update] → The conflict message says what is wrong; splitting the line restores it. Nothing is released yet, so no migration (see the project rule that nothing is breaking before a first release).
- [Packets in flight when the conflict starts] → They keep landing; only new deliveries are held. Requests count what lands as before.
- [Cost of walking members on every call] → Unchanged from `reachable` today.

## Migration Plan

None. The conflict is derived, nothing new is saved.
