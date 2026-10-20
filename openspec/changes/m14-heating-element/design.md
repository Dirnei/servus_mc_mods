## Context

See proposal.md for the why. Decided in the explore of 2026-10-20 (`temp/delicacies-brief.md`, Foundation parts): the Heating Element is a Foundation upgrade item for fireboxes; with it a firebox takes FE through a small flat iron power port instead of burning fuel; look is the subtle version of mockup option D (warm orange coils over the dark door slits, small flat iron port on the side, no cable stub) plus a round brass thermometer on the front that only appears with the element, whose needle climbs while the firebox preheats; cooking starts once it is hot; it cools slowly when power stops; a wood fire has no gauge, is hot at once and goes out when the fuel ends; the Alchemy Calciner never takes it; no separate heat API, FE is the shared interface.

`m14-tiled-stove` and `m15-brewhouse` (applied, not archived) left the seam: `firebox/HeatSource` with `tick(demanded)`, `hot()`, `heat()`; `FuelFire` behind it; both block entities hold `private final HeatSource heat = fire`. Sources checked: `minecraft-patched-26.1.2.116-sources.jar`, `neoforge-26.1.2.116-sources.jar`, and the family code named below.

## Goals / Non-Goals

**Goals:** the element item, install and removal by hand, FE heating with a data driven preheat curve behind the existing seam, the port as the only FE face, the gauge with a smooth needle and sparse sync, the coil look, the same on the Brewhouse, JUnit tests for the pure parts and GameTests for the behaviour.

**Non-Goals:** see proposal.md.

## Decisions

### D1 The item (Foundation, package `upgrade`)
`HeatingElementItem extends Item` with a gray tooltip line (`item.servus_foundation.heating_element.tooltip`), registered as `heating_element` in `ServusFoundationItems` (stacks to 16) and added to the Servus tab after the Capacity Upgrade. It is not an `UpgradeItem` and not in the `servus_foundation:upgrades` tag, so `Upgrades.isUpgrade` is false and `UpgradeSlots` refuse it; `CapacityUpgradeSlots` take only the Capacity Upgrade. Recipe: `CCC` / `RIR` / `CCC` (Copper Ingot, Redstone, Iron Ingot), unlocked by copper. Item model `basicItem`. Fireboxes check `stack.is(ServusFoundationItems.HEATING_ELEMENT.get())`; Foundation knows nothing of fireboxes.

### D2 Pure heat logic (Delicacies, package `firebox`, no Minecraft imports)
- `HeatingStats(int energyPerTick, int energyBuffer, int heatUpTicks, int coolDownTicks, float hotAt)` with `heatUpPerTick()` (`1 / heatUpTicks`), `coolDownPerTick()`, and a `Partial` of optionals with `over(defaults)` (clamped: ticks at least 1, `hotAt` in 0.01 to 1), the shape of Foundation's `DriveStats`. Defaults `TILED_STOVE` (20, 4000, 200, 1200, 0.6).
- `EnergyDraw` (`boolean draw(int amount)`: takes the amount from the buffer if it holds it all).
- `ElectricHeat implements HeatSource`: a temperature from 0 to 1. `tick(demanded)`: powered when demanded and `draw(energyPerTick)` succeeds; powered adds `heatUpPerTick`, otherwise subtracts `coolDownPerTick`, clamped. `hot()` is `heat > 0 && heat >= hotAt`. `rate()` is the change per tick the curve follows now (positive while powered below 1, negative while cooling above 0, else 0), for the client needle. `restore(float)`.
- `FireboxHeat implements HeatSource`: the firebox's switch. It holds a `FuelFire` and an `ElectricHeat` and an `element` flag and delegates to the one in use. `installElement()` (false when it has one; puts the fire out with `restore(0, 0)` and starts the element cold), `removeElement()` (false when none; the temperature drops to 0), `hasElement()`, `rate()`, `fire()`, `electric()`. The stats come in as a `Supplier<HeatingStats>`, so a datapack reload applies at once.
- `GaugeSync`: `boolean changed(float heat, boolean hot, float rate)` is true when the coarse step (`floor(heat * 16)`), `hot` or the sign of `rate` differs from the last reading it reported. So a stove heating in 10 s syncs about 16 times, a cooling one about every 4 s, and a stove at full heat not at all.
- `GaugeNeedle`: `shown(heat, rate, ticksSince)` (the synced reading carried on by its rate, clamped to 0..1) and `angle(shown)` in degrees, `(shown - 0.5) * 240`, so cold points 120 degrees left of up and full heat 120 degrees right, as the mockup's dial ticks run.

JUnit: `ElectricHeatTest` (cold at start; rises only while demanded and powered; draws exactly `energyPerTick` per heating tick; not hot below the threshold, hot from it; full after `heatUpTicks`; cools by `coolDownPerTick` without power or without demand; never below 0 or above 1; rate per state; restore), `FireboxHeatTest` (without element the fire burns fuel as before; with element no fuel is asked; install puts a burning fire out; second install refused; remove drops the heat; heat and hot follow the source in use), `GaugeSyncTest` (first reading reports; same step quiet; new step, threshold or turn reports), `GaugeNeedleTest` (carry on by rate, clamp, angles at 0, 0.5, 1), `HeatingStatsTest` (partial over defaults, clamping).

### D3 Minecraft side of the firebox (Delicacies, package `firebox`)
`FireboxHeating` is what both block entities own instead of their `FuelFire`: a `FireboxHeat`, an FE buffer (Foundation `MachineEnergyStorage`, a NeoForge `SimpleEnergyHandler`, verified `SimpleEnergyHandler(int capacity, int maxInsert, int maxExtract)`; insert unlimited up to the capacity, extract 0; `EnergyDraw` is its `consume`), the `GaugeSync`, and the client reading (heat, rate, game time it arrived). Methods: `heat()` (the `HeatSource` the block entity stores in its `heat` field, so the seam stays), `tick(demanded)` (sets the buffer capacity from the stats, ticks, and returns whether clients need the gauge), `install()`, `takeElement()` (the item, or empty), `dropElement(level, pos)`, `energy()` (the buffer while the element is in, else null), `acceptsFuel()`, `save`/`load` (`Element`, `BurnLeft`, `BurnTotal`, `Heat`, `Energy`), `saveGauge`/`loadGauge` for the update tag, and `shown(gameTime, partialTick)` for renderers. Numbers come from `HeatingStatsManager` (`SimpleJsonResourceReloadListener` on `data/<ns>/heating/`, one file per firebox id, merged over the firebox's code defaults like `DriveStatsManager`; `override(id, stats)` for GameTests; registered with `AddServerReloadListenersEvent`). Shipped: `data/servus_delicacies/heating/tiled_stove.json` and `data/servus_brewery/heating/brewhouse.json`.

`ElementUse` holds the shared hand rules so stove and Brewhouse behave alike: `install(level, pos, player, stack, install)` (runs the block entity's install; consumes one unless creative, sound, overlay `message.servus_delicacies.firebox.element_in` or `.element_already`) and `takeOut(level, pos, player, takeOut)` (`Inventory#placeItemBackInInventory`, `.element_out`, or `.no_element`); and `fuelRefused()` (`.fuel_refused`). The boxes of D6 live in the pure `firebox/HeaterParts`.

### D4 Tiled Stove
`StoveTileBlock` gains `HEATER` (the stove holds an element; on every tile) and `PORT` (this tile carries the port: set on forming on the tile at the row's `from` offset, the end toward `front.getCounterClockWise()`, which is right seen from the front). `lightLevel`: door, formed and lit gives 13 with a wood fire and 7 with the element. `animateTick` shows flames and crackle only without `HEATER`.

`StoveTileBlockEntity`: `fire` and `heat = fire` become `heating = new FireboxHeating(...)` and `heat = heating.heat()`; fuel insertion (`addFuel`, the capability view) checks `heating.acceptsFuel()`; `serverTick` sends a block update (`sendBlockUpdated`, `UPDATE_CLIENTS`) when `heating.tick` says so; `unform` drops the element; install and removal set `HEATER` on every tile and invalidate the capabilities (so ducts see the port appear). `getUpdateTag` writes only the gauge (`ValueIo.write`), `getUpdatePacket` is `ClientboundBlockEntityDataPacket.create(this)`, as `KitchenBlockEntity`. `energyCapability(side)`: the master's buffer on the port tile when `side` is the port face or null, else null; registered for `Capabilities.Energy.BLOCK` (verified `BlockCapability<EnergyHandler, @Nullable Direction>` in 26.1 `Capabilities.Energy`) on the stove tile type. Status by empty hand with the element: `message.servus_delicacies.stove.electric.lit` / `.cold` with heat percent and stored FE.

Hand use: a Heating Element on a formed tile installs (before the fuel rule); a sneaking empty hand takes it out; fuel with the element is refused with a message.

### D5 Brewhouse
`BrewhouseBricksBlock` gains `HEATER` (768 states: one model reference each, about thirty models). `BrewhouseBlockEntity` swaps its fire the same way (stats id `servus_brewery:brewhouse`, defaults 40, 8000, 300, 1600, 0.6), sets `HEATER` on the six bricks on install and removal, drops the element on unform, gets the same update tag and packet. Energy: the master's buffer on the `KILN_BACK` brick when `side` is `front.getCounterClockWise()` or null. Hand use on bricks: the element installs (any brick, before trays and fuel); a sneaking empty hand takes it out (bricks only; kettle blocks keep their sneaking rule). Light 7 with the element; no flames.

### D6 Models (pixels, built facing north)
Stove Tile, formed: one model per `DOOR`, `LIT`, `HEATER` and `PORT`, named `stove_tile_<door|formed>[_lit][_heater][_port]`, cached as the Brewhouse does. With `HEATER` the door face is `stove_tile_door_heater` (lit) or `stove_tile_door_heater_cold`, and the door model adds the dial:
- `DIAL` 5.5,10.5,-0.25 to 10.5,15.5,0: north `#gauge` (UV 0,0,16,16), west, east, up and down `#gauge` too with UVs on the dial's outer ring (column 14 or 1, row 1 or 14, half a pixel wide), so the edges show brass only where the round dial reaches them; no south face.
- `PORT` -1,4,5 to 0,10,11 on the port tile: west `#port` (UV 0,0,16,16), north, south, up, down `#iron`; no east face.

Brewhouse (structure pixels, `BrewhouseModel`): `GAUGE_DIAL` u 26.5..31.5, v -0.25..0, y 3.5..8.5 (front stretched `#gauge`, other drawn sides on the ring as above, no back), in `KETTLE_FRONT_RIGHT`; `POWER_PORT` u 48..49, v 21..27, y 3..9 (right `#port` over the whole texture, front, back, up, down `#iron`, no left), in `KILN_BACK`. Both are in `HEATER_PARTS`, added to a cell's pieces only for `HEATER` states; the shapes keep using `PARTS`; `BrewhouseModel.fixedUv` gives the ring and port UVs that do not follow the slicer's position rule. Door texture with `HEATER`: `brewhouse_door_heater` (lit) or `_cold`.

Needle (standalone model `servus_delicacies:block/gauge_needle`, pivot at pixel 8,8,0): `NEEDLE` 7.8,7.6,-0.15 to 8.2,10.2,-0.1 (`#needle`, no south face) and `HUB` 7.55,7.55,-0.27 to 8.45,8.45,-0.19 (`#hub` brass, no south face). The renderer moves the pivot onto the dial centre in front of the dial face (stove: 8, 13, -0.25; Brewhouse: the slicer's local point of u 29, y 6) and turns it about Z by `GaugeNeedle.angle`.

Overlapping boxes (CLAUDE.md "Models and textures"):

| Pair | Shared plane? | How |
|---|---|---|
| dial / tile or brick front (z 0) | no | the dial has no south face |
| port / tile or brick end face | no | the port has no inner face |
| needle / dial face (z -0.25) | no | the needle's back is at -0.35 after the move (0.1 px in front) |
| hub / needle | no | hub back at -0.44, needle front at -0.4 (0.04 px apart), and neither draws its south face |
| Brewhouse gauge / kettle door | no overlap | gauge u 26.5..31.5, kettle door ends at u 24, kiln door starts at u 34 |
| Brewhouse port / kiln tray | no overlap | the tray stands on top (y 12 up), the port is at y 3..9 |
| dial edges / dial front | no | perpendicular faces of one box |

### D7 Gauge renderer (Delicacies client)
`GaugeRenderer` (a `BlockEntityRenderer` with its own render state, like Foundation's `ElectricMotorRenderer`): `extractRenderState` asks its `GaugeRenderer.Host` for a `Reading` (facing, dial centre, heat): Delicacies registers it for the stove tile type (drawn on a formed door tile with `HEATER`), Brewery for the Brewhouse type (drawn on `KETTLE_FRONT_RIGHT` with `HEATER`; the reading comes from the master, found from the cell's position in its block state). `submit` turns to the facing (`ModelPoses.faceFromNorth`) and submits the needle with `ModelKeys`' way (`collector.submitBlockModel` with `Sheets.cutoutBlockSheet()`, as Foundation's `ModelKeys.submit`). The needle key is registered in `ModelEvent.RegisterStandalone` (verified in Foundation `MachineClient`) with a Delicacies id. The client reading is set in `loadAdditional` from the update tag with the client's game time; between updates `GaugeNeedle.shown` carries it on with the partial tick.

### D8 Textures (`tools/heating-textures/gen.py`)
One new generator for the parts of this change across three mods, reading the existing door textures and drawing over them:
- Foundation `textures/item/heating_element.png`: port of the mockup sprite `heating_element`.
- Delicacies `textures/block/stove_tile_door_heater.png` and `_cold.png`: the cold `stove_tile_door.png` with the mockup's `coil` at x 4..11, rows 9 and 12 (warm `#ffd08a`, `#ff8a2a`, `#c8401a`; cold `#7a4a36`, `#5a3426`, `#3e241c`).
- Brewery `textures/block/brewhouse_door_heater.png` and `_cold.png`: `brewhouse_door.png` with the coil at x 4..11, rows 7 and 10 (mockup `herd_front_elec`).
- Delicacies `gauge_dial.png` (the mockup `gauge` without the needle: cream face, two brass rings, seven ticks with the last two red, a brass hub), `gauge_needle.png` (near black with a light edge), `firebox_port.png` (heavy iron plate, brass rim, two dark socket slots).

### D9 GameTests
Delicacies `HeatingElementGameTests` (listed in `DelicaciesGameTests`): `elementInstallsShowsPortAndTakesFe` (one element used, `HEATER` and `PORT` states, energy only on the port face), `elementStoveCooksWithoutFuel` (Coal in the slot first, element, Pot with potatoes and water, FE every tick: Boiled Potatoes appear, Coal untouched, FE was drawn), `noPowerCoolsAndStops` (heat restored just above the threshold, no FE: the stove goes cold and the Pot's progress stops), `elementRefusesFuel`, `fuelComesOutWhileTheElementHeats` (Coal in the slot, element in: the fuel capability gives the Coal out, which it does not without the element), `elementDropsOnUnform`, `sneakingEmptyHandTakesTheElement`, `noElementNoEnergy`. Brewery `BrewhouseGameTests` gains `brewhouseWithElementBrews`, `brewhouseElementDropsOnUnform`, `brewhousePortIsTheOnlyEnergyFace`. Foundation `UpgradeGameTests` gains `heatingElementIsNoUpgrade` (speed and capacity slots refuse it).

### Settles from PLAN.md
PLAN.md lists `m14-heating-element` (Foundation, after the Tiled Stove) and decides the look and that the Calciner never takes it; no open decision of section 5 is touched. Decided here: install by right click, removal with a sneaking empty hand, the element's numbers per firebox in `heating/<firebox>.json`, the port on the right end, the gauge above the stove door and between the Brewhouse doors, and the sync rule. PLAN.md is not edited in this change.

## Assumptions

- No slot GUI: no firebox has a GUI, so installing is a right click (the brief's simple option), removal a sneaking empty hand click (the carrot's sneak click already dismantles a block).
- With the element, fuel already in the slot stays and burns again once the element is out; new fuel is refused. Automation may take that fuel out while the element is in (without it, only what no longer burns comes out), so it is never stuck. Installing puts out a burning wood fire, and its item is lost.
- The element heats only while a spot asks for heat, like the wood fire lights only then; it draws FE only on those ticks.
- Preheat: 0 to full in 10 s, full to cold in 60 s, hot from 60 percent, 20 FE per tick with a 4000 FE buffer for the stove; the Brewhouse 15 s, 80 s, 60 percent, 40 FE per tick, 8000 FE (all data).
- The temperature and stored FE are lost when the element comes out; the element item carries no data.
- Coils glow warm while the firebox counts as hot, dull while it preheats or cools below the threshold; the soft light is 7.
- The port faces right seen from the front on both fireboxes; FE goes in only on that face (or with no side, for tools).
- The stove's gauge sits on the door tile above the door rather than on the next tile as in the mockup, so the gauge, its reading and its renderer stay on the master; the Brewhouse's sits between the doors as in the mockup.
- Recipe: six Copper Ingots, two Redstone, one Iron Ingot give one Heating Element.

## Risks / Trade-offs

- [Other mods' tools without a side] They see the buffer through the null side; that is the NeoForge convention.
- [A duct already beside the port when the element goes in] The block state change and the capability invalidation make the duct look again.
- [Edits to two unarchived changes' block entities] The stove and Brewhouse block entities change only at the fire field, fuel checks, unform, the tick and the new update tag; `m14-meat-dishes`, `m14-sides` and `m15-beer` do not touch them.

## Migration Plan

None. Nothing is released.
