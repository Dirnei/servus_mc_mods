# Proposal

## Why

Play test (2026-10-07): where a two line duct bends, branches or crosses, no direction of the colour bands matches every arm, and the centre looks broken whichever way it is painted. A junction should read as one plain block.

## What Changes

- A two line centre whose arms all lie on one axis keeps its bands along that axis, now for east west, north south and vertical runs alike.
- A two line centre with arms on more than one axis is a uniform junction cube in a neutral duct colour.

## Capabilities

### Modified Capabilities
- `logistics/duct-appearance`: band direction per axis and the junction cube.

## Non-goals

- Changing single line ducts, whose centre is uniform already.

## Impact

- `logistics`: centre parts per axis plus a junction part with a new neutral texture, chosen by the dynamic model from the arm axes.
