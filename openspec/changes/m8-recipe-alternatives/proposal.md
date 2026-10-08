# Proposal

## Why

During the M8 play test the user asked the Counter Terminal for charcoal and the order needed acacia logs, although the warehouse held other logs. Several recipes make the same item (every log smelts to charcoal, so a furnace adapter or a Burner Smelter reports one recipe per log; several crafting patterns can make one item), and the Foundation order planner always takes the first recipe that makes the wanted item. This change makes the planner choose between alternatives by what is in stock. It is a follow up of the M8 items `m8-crafting-cpu` and `m8-machine-adapter` from PLAN.md.

## What Changes

- The pure Foundation order planner, when more than one recipe makes the wanted item:
  - first uses alternatives whose inputs are fully in stock (after reservations and surplus), the one with the most batches covered by stock first, and splits the amount across alternatives when one alone is not enough (10 charcoal from 6 oak logs and 4 birch logs);
  - only then falls back to alternatives whose inputs must be produced, preferring a plan with nothing missing and the fewest new steps;
  - when every alternative misses something, reports the missing material of the alternative that misses the least;
  - breaks ties by recipe order, so plans stay deterministic.
- A single recipe for an item plans exactly as before; reservations, surplus reuse and the depth limit stay.
- Applies to producer recipes from several machines, to adapter recipes per ingredient and to several crafting patterns for one item alike.
- The Counter Terminal's craft preview already lists stock per item; with this change it shows the alternatives actually used (for example 6 Oak Log and 4 Birch Log).

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `foundation/production-api`: the order planner chooses between alternative recipes by stock.
- `storage/counter-terminal`: the craft preview lists the alternative ingredients used.

## Non-goals

- A per item preference setting ("always use oak for charcoal") in the Production Terminal: possible follow up after play testing.
- Tag based single recipes (one recipe that accepts any log) in producer reports.

## Impact

- `foundation/.../production/OrderPlanner.java` and its JUnit tests.
- Storage GameTests for charcoal through a furnace adapter.
- No API, data or save format change.
