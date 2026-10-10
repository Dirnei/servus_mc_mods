# Design

## Context

See proposal.md for the motivation. Current state:

- `PumpBlock` (abstract, extends `MachineBlock`) holds the compact shape, the five stub properties (`north`, `south`, `east`, `west`, `up`) and the stub logic. `givesWater(state, face)` excludes the front and `DOWN`; it decides stubs and, in `MachineEvents`, the Electric Pump's energy faces.
- The fluid and item capabilities go through side modes. `MachineBlockEntity` locks `RelativeSide.FRONT` to `DISABLED`, `PumpLogic` locks `BOTTOM`.
- Burner Pump models are datagen multiparts in `MachineDataParts.pump(...)`: one body per facing and lit state, plus stub parts. Its block entity renderer is `SideMarkerRenderer`, which draws nothing useful for a pump since it has no side config.
- `SinkRenderer` is the template for a moving part: a standalone model registered in `ModelEvent.RegisterAdditional`, drawn by a BER with `renderModel`, rotated by facing like the blockstate, with a widened `getRenderBoundingBox`.
- The Sink and the Calciner spawn `CAMPFIRE_COSY_SMOKE` from `Block.animateTick`.
- `LIT` is already a blockstate property, so the client knows when the burner runs without extra sync.

## Goals / Non-Goals

**Goals:**
- A two block Burner Pump that behaves exactly like today's pump from its lower half.
- A beam and rod that move only while lit, with no per tick server work and no extra network traffic.
- Reuse the stub, side mode and capability code instead of forking it.

**Non-Goals:**
- Changing the Electric Pump in any way.
- Exact pixel art. The layout below fixes where the parts go so they do not collide with stubs or the front; the play check tunes the rest.

## Decisions

### D1. One block with a `half` property, the vanilla door pattern

`BurnerPumpBlock` adds `BlockStateProperties.DOUBLE_BLOCK_HALF` (`DoubleBlockHalf.LOWER`, `UPPER`), mirroring `DoorBlock` and `DoublePlantBlock` in 1.21.1:

- `getStateForPlacement` returns null when `context.getLevel().getBlockState(pos.above()).canBeReplaced(context)` is false, so placing fails and keeps the item. Otherwise it returns the lower half.
- `setPlacedBy` sets the upper half above with the same `FACING` (flag `UPDATE_ALL`), then calls `super` for the owner.
- `updateShape`: on the lower half, a change from `UP` that is not this block's upper half turns the lower half into air; on the upper half, the same for `DOWN`. The upper half copies `FACING` from the lower half when the lower half changes. `LIT` is set with `UPDATE_CLIENTS` only, so it never reaches the upper half; nothing needs it there, since the renderer and the smoke read the lower half. Returning air from `updateShape` goes through `Block.updateOrDestroy`, which destroys the block with drops. So breaking the top drops the lower half's loot and runs its `onRemove`, which drops the fuel.
- `playerWillDestroy` in creative or without the right tool removes the bottom without drops, like `DoorBlock` with `DoublePlantBlock.preventDropFromBottomPart` (protected there, so it is copied). The fuel still spills in creative, as with every machine.
- `newBlockEntity` returns null for the upper half. `getTicker` is already filtered by type. `onRemove` only drops contents when a block entity exists.
- `getPistonPushReaction` is `PushReaction.BLOCK` for both halves. The lower half already resists through its block entity; the upper half has none, so it needs this.
- `useWithoutItem` and `onWrench` on the upper half redirect to `pos.below()`. The sneak dismantle removes the lower half, and the upper half follows through `updateShape`.

The 1.21.1 signatures are checked against the decompiled `DoorBlock` and `DoublePlantBlock` sources in the ModDevGradle workspace during task 1.1, not assumed.

**Alternatives:**
- A separate upper block, like the Elevator's parts: two registry entries and a second loot table for no gain.
- A model that draws past the block edge: the outline and collision cannot follow it, and you could walk through the chimney.

### D2. The top face of the lower half is closed like the bottom

`PumpBlock.givesWater(state, face)` becomes an instance hook, `opensFace(state, face)`, which `BurnerPumpBlock` overrides to also exclude `UP` and to return false for every face of the upper half. The stub code and the energy registration in `MachineEvents` call the hook through the block. `BurnerPumpBlockEntity` additionally locks `RelativeSide.TOP` to `DISABLED`, next to the existing front and bottom locks, so the fluid and item handlers report null upward. The `up` stub property stays on the shared block definition and is simply always 0 for the Burner Pump.

The upper half has no block entity, so `level.getCapability(...)` against it returns null for every capability. Ducts already decide arms by capability presence, so they show no arm toward it. That needs no code.

### D3. Static parts in the models, beam and rod in a renderer

Models are built by datagen in `MachineDataParts`, facing north and rotated by the blockstate like today:

```
 front view (facing north, the player looks at the fire door)

 y32 +----------------------------+
     |  ||                        |   upper half:
     |  ||   =====beam=====       |   chimney  x 11..14, z 11..14 (back right), up to y 31
     |  ||  /     ^pivot     \    |   cylinder x 2..6, z 5..9 (left), y 16..24
     |  || [cyl]  [post]     |rod |   pivot post centre, z 6..8, y 16..26
 y16 +--||-[   ]-------------|----+
     |  ||[====boiler====]   |    |   lower half:
     |  ||[  FIRE DOOR   ]  [|]   |   base plate 1..15 x 0..2 (as today)
     |    [==============]  [|]   |   boiler x 3..13, z 3..13, y 2..16, front texture on the north face
 y0  +====base plate==============+   rod x 11..13, z 1..3 (front right), y 2..24, drawn by the BER
```

- The rod stays in the front right corner, outside the stub boxes, which sit centred on each face (pixels 5..11) and only 3 pixels deep. The front never shows a stub.
- The chimney is part of the upper half's static model and is not drawn per tick.
- Shapes: lower half = base plate + boiler + stubs; upper half = chimney + cylinder + pivot post. The beam and rod are left out of collision because they move.

The beam and the rod are two standalone models, `block/burner_pump_beam` and `block/burner_pump_rod`, registered in `MachineClient.onRegisterAdditionalModels` next to the Sink fan. `BurnerPumpRenderer` draws them from the lower half's block entity, following `SinkRenderer`:

- Rotate by facing like the blockstate.
- Rotate the beam about the Z axis through the pivot.
- Translate the rod vertically by the beam end's height.
- `getRenderBoundingBox` spans both blocks.
- Light is sampled at the upper half, where most of the beam is.

The renderer replaces `SideMarkerRenderer` for `BURNER_PUMP_ENTITY`, since the pump has no side config to mark.

### D4. Beam motion is a pure function with a client side phase

`BeamStroke` (package `machine/pump`, no Minecraft imports) maps a phase in ticks to the beam angle, `amplitude * sin(2 * pi * phase / period)`. It also gives the rod's vertical offset from the angle and the lever arm.

The renderer keeps the phase on the block entity as a client only field, like `SinkBlockEntity.fanAngle`. It advances by the elapsed partial time while `LIT` is true and stays where it is while unlit, so the beam freezes in its pose instead of snapping back.

Amplitude (example 12 degrees) and period (example 40 ticks) are look constants in the renderer, not balance data, since the spec says the motion does not depend on balance.

### D5. Smoke from `animateTick`

`BurnerPumpBlock.animateTick` on the lower half, only when `LIT`, spawns `CAMPFIRE_COSY_SMOKE` with `addAlwaysVisibleParticle` at the chimney top (`pos.above()` plus the chimney's top centre, rotated by facing), with a chance per call (example 0.5) and a small random offset. This is the same API as the Sink, client side, with no server work.

`animateTick` is a `Block` method called from `ClientLevel.animateTick` for blocks near the player.

### D6. Item model shows the whole engine

The item model is a datagen model with the lower and upper elements stacked to y 32. Block model element coordinates may run from -16 to 32. Its GUI and ground display transforms are scaled to about 0.4 so it fits a slot. The beam is included in a level pose so the icon reads as an engine.

### D7. Loot

`MachineBlockLoot` stops using `dropSelf` for the Burner Pump. It uses `createSinglePropConditionTable(block, DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)`, the vanilla door helper in `BlockLootSubProvider`, so only the lower half drops the item.

### PLAN.md open decisions

None of section 5's open decisions are touched. This change adds M9 item 1b and updates the pumps paragraph.

### Pure versus Minecraft

- Pure Java with JUnit: `BeamStroke` (angle bounds, period, rod offset sign and size, zero at phase 0).
- GameTests in `PumpGameTests`: two block placement, no room above, breaking either half drops one pump, the upper half has no capabilities, the closed top face, the GUI opening from the top.
- Manual in game: the look, the beam motion, the smoke and the item icon.

## Risks / Trade-offs

- [Breaking the top in survival might drop nothing or twice] → Mirror the vanilla door code exactly. A GameTest breaks each half with `destroyBlock(pos, true)` and counts item entities.
- [GameTests set blocks with `setBlock`, which skips `setPlacedBy`, so the upper half would be missing and `updateShape` would remove the lower half when its neighbour updates] → The `burnerPump` test helper places the upper half explicitly, and `MachineBaseGameTests` does too for the Burner Pump.
- [The rod in the front right corner could clip a duct stub on the right side at stub size 2] → The rod box (x 11..13, z 1..3) and the side stub box (z 5..11) do not overlap. The play check confirms it.
- [The beam frozen mid stroke looks odd when idle] → Accept for now. Easing back to level is a renderer only change if the play check asks for it.
- [Existing worlds with a one block Burner Pump] → The project is unreleased, so there is no migration. A placed pump without an upper half shows the new lower half and removes itself on its next neighbour update. Players re-place it.
