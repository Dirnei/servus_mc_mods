# Proposal

## Why

Factories make waste: surplus grit, overflowing water from pumps, generators running with nothing to power. There is no way to get rid of items, fluids or energy except by hand. The user asked for a Sink: a trashcan for all three that, in the fiction, turns everything into heat and blows it out of a brick chimney.

Milestone: adds the item "Sink and Chimney" to M3 (Manufacturing Tier 1 completion), proposed after M5 at the user's request like the pumps. PLAN.md is updated in the same change.

## What Changes

- New Manufacturing block **Sink**: a brick block with a fan behind a grate on its front. It accepts items, fluids and FE through `IItemHandler`, `IFluidHandler` and `IEnergyStorage` and voids everything it accepts.
- New Manufacturing block **Chimney**: brick blocks stacked straight on top of a Sink form its chimney. The column joins visually and the top block carries a cap.
- **Chimney height sets the void rate.** With no chimney the Sink voids at its base rate per resource; every chimney block raises the rate; from a configured height on (example 5 blocks) the Sink voids without limit.
- While the Sink voided something recently it is active: the fan spins (faster with a taller chimney) and smoke rises from the top of the chimney, or from the Sink itself without one.
- Right click opens a small GUI with one trash slot that voids whatever is put in, the chimney height, the current rates and the shared side panel. A filled bucket used on the Sink empties into it. Hand input is not rate limited.
- Side modes Input and Off; the front (fan) is fixed Off.
- Rate tables per chimney height and the active time live in `data/servus_manufacturing/sink/sink.json` (examples: items 4, 8, 16, 32, 64 per second; fluid 50 to 800 mB per tick; energy 100 to 1,600 FE per tick; unlimited from 5 chimney blocks).
- Both blocks are hand crafted from bricks and iron.

## Capabilities

### New Capabilities
- `manufacturing/sink`: the Sink and Chimney blocks, voiding over the three capabilities, rate by chimney height, active state with fan and smoke, trash slot GUI, buckets, side modes, crafting.

### Modified Capabilities
None.

## Non-goals

- Using the heat for anything (no heat output, no generator coupling).
- Filters (void only certain items), a void counter or statistics.
- Chimneys of multiple columns or wider than 1x1, chimneys feeding from more than one Sink.
- Listing the Sink in the docs site game data export.
- Pollution or any world effect of the smoke.

## Impact

- `manufacturing`: new `machine/sink` package (`SinkBlock`, `SinkBlockEntity`, `ChimneyBlock`, handlers, pure `VoidBudget` and `SinkRates`), `SinkStatsManager` reload listener, `SinkMenu` and `SinkScreen`, `SinkRenderer` (fan plus side markers), registry entries, capability registration, datagen (blockstates, models, loot, recipes, lang), textures, `sink/sink.json`, GameTests.
- `PLAN.md`: section 2.1 and M3 gain the Sink.
- Built while `m5-fluid-tank`, `m5-fluid-pipe` and `m3-pumps` are open; the Sink only uses NeoForge's `IFluidHandler`, so its GameTests use buckets and a direct capability call and do not wait for those changes.
