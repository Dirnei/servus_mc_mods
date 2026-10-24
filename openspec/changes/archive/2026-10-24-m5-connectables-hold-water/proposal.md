# Proposal

## Why

Flowing water and lava wash away the Utility Duct, the Warehouse Rail (and the Farm Rail built on it) and the Elevator shaft segment, dropping them as items. This is vanilla behaviour for thin blocks that never opted out, not a decision of ours, and it breaks pipe runs next to ponds and flooded warehouses. This belongs to PLAN.md milestone M5 Logistics (Utility Duct) and carries over to the M6 warehouse rails, which share the same block base.

## What Changes

- Ducts become waterloggable: placed into a pond they keep the water, buckets fill and empty them, and a broken waterlogged duct leaves its water. Dry ducts hold back flowing water and lava; a duct is never placed into a lava source.
- Rails, farm rails and elevator segments hold back water and lava like a wall: fluid neither flows into them nor washes them away. They are not waterloggable.
- Rails, farm rails and elevator segments can not be placed into a water or lava source block. The placement fails silently, the source stays.
- Placing any of them into flowing water or lava is allowed; the block is placed dry and dams the flow.
- A bucket used on a rail or segment places its fluid next to it, as on stone.

## Non-goals

- Waterlogging rails, elevator segments or any other Servus block.
- Other thin blocks (lamps, side ladder, machines with partial shapes). They are checked separately if they turn out to wash away.
- Any interaction between a duct's Fluid Pipe and the water around it.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/utility-duct`: the duct is waterloggable and holds back other fluids.
- `shuttles/warehouse-rail`: rails (and with them farm rails) hold back fluids and can not be placed into a fluid source.
- `shuttles/rail-elevator`: shaft segments hold back fluids and can not be placed into a fluid source.

## Impact

- `foundation/.../block/ConnectableBlock.java`: the shared base of duct, rail and elevator.
- `foundation/.../logistics/duct/UtilityDuctBlock.java`: waterlogging.
- `shuttles/.../rail/RailBlock.java`: its placement override builds on the base state and must pass a refused placement through.
- GameTests in Foundation and Shuttles for the new behaviour.
- `foundation/CHANGELOG.md` and `shuttles/CHANGELOG.md` at archive.
