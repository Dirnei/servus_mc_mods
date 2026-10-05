# Design

## Context

Manufacturing has the machine framework (burner tier). Lib has the network manager with no user yet. See proposal.md and the delta specs.

APIs verified in the NeoForge 21.1.255 sources:
- `IEnergyStorage` (`receiveEnergy`, `extractEnergy`, `canExtract`, `canReceive`), `EnergyStorage(capacity, maxReceive, maxExtract)` with protected fields, `Capabilities.EnergyStorage.BLOCK`.
- `BlockCapabilityCache.create(BlockCapability, ServerLevel, BlockPos, context)` and `getCapability()`.
- `BlockBehaviour.updateShape(state, direction, neighborState, LevelAccessor, pos, neighborPos)`, `getShape`, `Block.box`, `Shapes.or`.
- `BlockStateProvider.getMultipartBuilder`, `ConfiguredModel.Builder.rotationX/rotationY`, `ModelBuilder.element().from().to().allFaces()`.

## Goals / Non-Goals

**Goals:**
- Distribution logic is pure and tested; cable and generator glue is thin.
- No per tick block scanning: cables keep capability caches that NeoForge invalidates for us.
- The powered base is ready for the Assembler and Lab without further framework work.

**Non-Goals:**
- Tiers, side modes, facades, batteries.

## Decisions

### D1: EnergyDistributor in Lib
`io.github.dirnei.mc.servus.lib.energy` with `EnergySource` (`extract(max, simulate)`), `EnergySink` (`receive(max, simulate)`) and `EnergyDistributor.distribute(sources, sinks, rate)`. Algorithm: simulate demand per sink and supply per source, both capped by the rate; budget is the smaller total; allocate the budget to sinks in rounds of equal shares (water filling, so a sink that needs less frees the rest for the others); then pull the allocated amounts from the sources in order and push to the sinks. Returns the energy moved.

### D2: Generator reuses the burner base
`CoalGeneratorBlockEntity extends BurnerMachineBlockEntity` with `workTicks` 1 and `finishWork()` adding `energyPerTick` to its buffer. `canWork()` is "buffer not full", so fuel is only consumed when energy is needed, and a burn in progress keeps counting down. `BurnerMachineBlockEntity` divides the item burn time by the new `fuelDivisor` stat (default 1; generator 4). After the burner tick the generator pushes energy to the six neighbours through `EnergyNeighbors`, a helper holding six capability caches.

### D3: Energy storages
`MachineEnergyStorage extends EnergyStorage` adds `consume`, `add`, `setCapacity`. Generator: receive 0, extract unlimited. Powered machines: receive unlimited, extract 0. Both are exposed through `Capabilities.EnergyStorage.BLOCK` on every side.

### D4: PoweredMachineBlockEntity
Mirror of the burner base: `canWork()` and `finishWork()` from subclasses; per tick, if work is available and the buffer holds at least `energyPerTick`, consume it and advance progress. Without energy the progress is kept. Buffer capacity comes from stats on first tick (stats are data and may not be loaded when the block entity is constructed). New container data indices `DATA_ENERGY` and `DATA_ENERGY_CAPACITY`.

### D5: Cable block and network
`PowerCableBlock` is a `BaseEntityBlock` with six boolean connection properties, shapes built from a centre box and six arm boxes, cached per state. `updateShape` recomputes only the property facing the changed neighbour: connected when the neighbour is a cable or offers the energy capability on that side. `PowerCableBlockEntity extends NetworkBlockEntity`, no ticker.
`PowerCableNetwork extends ServusNetwork` is registered as `NetworkType` `servus_manufacturing:power_cable` with `ConnectionRule.always()` (cable touching cable always connects). On node join it creates six `BlockCapabilityCache`s for the node; on leave it drops them; on absorb it takes over the other network's caches. Each tick it classifies the cached capabilities into sources (`canExtract`) and sinks (`canReceive` and not `canExtract`) and calls the distributor with the cable rate (`energy_per_tick` from `data/servus_manufacturing/machine/power_cable.json`, default 256).
Alternative: cables expose an energy capability and relay like most pipe mods. Rejected because the network already knows all endpoints; relaying adds buffering and ordering artifacts.

### D6: GUI
`BurnerMachineScreen` becomes `MachineScreen` with a `Layout` (flame, arrow, energy bar). The energy bar is drawn with filled rectangles at the right side of the shared background and shows a tooltip with stored and maximum FE. The generator menu has the fuel slot only.

### D7: Models
Generator: orientable with lit front like the other machines. Cable: multipart with `power_cable_center` always and `power_cable_arm` per side with rotations (north arm unrotated; east 90, south 180, west 270 around Y; up 270 and down 90 around X). Item model: `power_cable_inventory` with centre and two arms.

## Pure Java vs Minecraft

- Pure Java, JUnit tested: `EnergyDistributor` and its interfaces.
- Minecraft, verified by server runs over RCON: generator fills its buffer, cable chain delivers energy to a powered test target. There is no powered machine yet, so the RCON test uses a second generator as source check and a `data get` on buffers; the first real consumer test comes with the Assembler. Client launch verifies models and screens.

## PLAN.md decisions settled

None. Numbers follow the Tier 1 table (40 FE/t, 1 coal per 20 s).

## Risks / Trade-offs

- [A storage that can both receive and extract is treated as a source only] -> Documented; batteries come later with a smarter rule.
- [Six capability caches per cable] -> Cheap objects, invalidated by NeoForge; no scanning.
- [Energy pushed by the generator and pulled by a cable in the same tick] -> Each transfer is atomic against the same buffer; no double counting.
