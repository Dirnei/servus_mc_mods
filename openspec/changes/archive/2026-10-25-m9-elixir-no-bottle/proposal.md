# Proposal

## Why

Drinking the Elixir of Sight hands back a glass bottle. The user wants the elixir to be used up entirely: the flask is part of the brew, not a container to refill.

Milestone M9 (Alchemy), Elixir of Sight. PLAN.md has no item for it: it is a small rule change to the archived elixir.

## What Changes

- Drinking the Elixir of Sight leaves nothing in the hand; no glass bottle comes back.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `alchemy/elemental-sight`: the Elixir of Sight no longer leaves a glass bottle.

## Non-goals

- The Receptaculum already uses up its Elixir without returning bottles; it stays as it is.
- No change to the elixir's effect, poison roll, stack size or Athanor craft.

## Impact

- `alchemy/sight/ElixirOfSightItem` (drop the use remainder) and its GameTest in `gametest/AlchemyGameTests`.
