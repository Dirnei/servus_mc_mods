# Design: m9-side-config-tabs

## Context

See proposal.md for why. Today `foundation.side.SideConfig` holds one `SideMode` per relative side. `SidedBlockEntity` exposes it, `SideConfigWrench` cycles it from the carrot, `SideConfigMenus` maps menu buttons `1000..1005` to sides, `MachineBlockEntity` syncs the six modes as container data `DATA_SIDE_FIRST..+5`, `SideConfigPanel` draws the 3 by 3 grid and `SideMarkerRenderer` draws frames while the carrot or goggles are active. Alchemy machines decide fluid exposure from the Items mode (`allowsExtract`, `allowsInsert`, `pushes`) through `PrincipleTanks` and `AlchemyMachines.push`.

The Fluid Pipe already separates fluids by channel per duct side; what is missing is a machine side that offers one tank only.

## Goals / Non-Goals

**Goals:**
- A second, independent Fluids layer per side for blocks with several tanks, configurable in the GUI and with the carrot.
- Unconfigured machines behave exactly as today (All).

**Non-Goals:**
- Energy layer, pipe buffers, per network fluid locks, an eject switch (see proposal Non-goals).

## Decisions

### D1: Layers as separate pure types, Items layer unchanged

`SideConfig` stays as it is and becomes the Items layer. A new pure `FluidSides` (package `foundation.side`) holds per relative side an assignment: `OFF`, `ALL` or a tank index, plus fixed sides, `cycle`, `set`, `isDefault` and `toBytes`/`fromBytes`. It is created with the tank count. A pure `SideLayer` enum names `ITEMS` and `FLUIDS`. Folding fluids into `SideMode` was rejected: the assignment depends on each block's tanks, and the item modes would grow a combinatorial list.

Cycle order: Off, All, tank 0 .. tank n-1, Off. Default All; fixed sides Off.

### D2: Blocks declare their tanks

`SidedBlockEntity` gets `default FluidSides fluidSides()` (null when the block has no Fluids layer), `default List<SideTank> sideTanks()` and `cycleFluidSide(RelativeSide)`. `SideTank` is a record of a translation key, an ARGB colour and a direction (`IN` or `OUT`). The Calciner declares Salt, Sulfur and Mercury as `OUT`, the Coagulator as `IN`, colours from `PrincipleFluids`.

### D3: Fluid exposure follows the Fluids layer

`PrincipleTanks` gets a view per side: for a side set to a tank it exposes a single tank handler (fill only for `IN`, drain only for `OUT`); for All the existing three tank handler; for Off null. The fluid capability providers of the Calciner and Coagulator ask the Fluids layer instead of the Items mode. `AlchemyMachines.push` pushes a tank on sides set to that tank or to All, no longer on Auto and Push sides.

### D4: Sync and menu buttons

`MachineBlockEntity` reserves six more data values after the Items sides (`DATA_FLUID_SIDE_FIRST`), so every later index keeps its relative position after `DATA_COUNT`. Blocks without a Fluids layer report 0 there. `SideConfigMenus` maps buttons `1000..1005` to Items sides (unchanged) and `1006..1011` to Fluids sides; `MachineMenu.clickMenuButton` routes them.

### D5: Panel tabs

`SideConfigPanel` takes the layers the block has. With more than one it draws small tabs above the grid (a chest icon for Items, a bucket for Fluids), keeps the selected tab in the screen, and draws the grid for that layer. Fluid cells use the tank colour, All a neutral blue and grey stripes, Off the existing Off colour; the first letter shows S, U, M or A.

### D6: Carrot layer

`ConfigurationCarrotItem` gets a data component `servus_foundation:carrot_layer` (the `SideLayer` name, default `ITEMS`) and overrides `use` for clicks on the air to switch it, with an action bar message. `SideConfigWrench.use` reads the carrot's layer: Fluids cycles the Fluids side when the block has one, else the Items side. The item tooltip names the layer.

NeoForge APIs: `DataComponentType` registration through `DeferredRegister.DataComponents` and `Item.use` (both already used in the repo, for example the zone filter component and the Zone Label's air click), `ContainerData` for sync.

### D7: Markers per layer

`SideMarkerRenderer` asks the local player's held carrot for its layer. For Fluids it draws frames in the assigned tank colour on tank sides, a neutral frame on All sides and nothing on Off sides. Goggles alone keep drawing the Items layer.

### Pure and Minecraft logic

Pure, JUnit: `FluidSides` (default, cycle order, fixed sides, encoding), `SideLayer`. GameTests: separate pipes per principle out of a Calciner; a Mercury side of a Coagulator refuses Salt; an unconfigured Calciner behaves as before; dismantle keeps both layers. Manual: tabs, colours, carrot layer switching and markers in the client.

## PLAN.md decisions settled

None of section 5. PLAN.md M9 gets the new item; section 2.1 gets one sentence that machines with several tanks assign a tank per side.

## Risks / Trade-offs

- [Shifting `DATA_COUNT` moves every later data index] → All later indexes are expressed relative to `DATA_COUNT`; a GameTest and the client check of the pump gauge and alchemy gauges confirm nothing moved.
- [Players expect energy sides too] → Out of scope; can be added later as a third layer with the same pattern.
- [Conflicts with the parallel burner Calciner work] → Apply this change after that work is merged; both touch the Calciner and Coagulator.

## Migration Plan

None, nothing is released. Placed machines load with every Fluids side at All.
