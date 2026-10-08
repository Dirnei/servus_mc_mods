# Proposal

## Why

Play test (2026-10-08): a rail running east to west always looks like a crossing. Its centre plate always paints the two steel lines north to south, so on an east west run the centre crosses the lines of the arms. Corners show the same crossing instead of a curve.

## What Changes

- The rail centre follows its connections: straight north south or east west, a curve when the two connected sides meet at a corner, and a `#` crossing (both line pairs) for three or four connections. A rail with one connection runs along that side's axis, a lone rail runs north to south.
- Docks and garage arms count as connections for the centre.

## Capabilities

### Modified Capabilities
- `storage/warehouse-rail`: the centre piece matches the connections.

## Non-goals

- Changing shuttle paths; this is rendering only.

## Impact

- `storage`: datagen in `RailModels.java` (centre straight, curve and cross models chosen by multipart conditions), two new textures `rail_curve.png` and `rail_cross.png`, regenerated blockstate and models.
