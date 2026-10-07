# Proposal

## Why

Factories make waste: surplus grit, overflowing water from pumps, generators running with nothing to power. There is no way to get rid of items, fluids or energy except by hand. The user asked for a Sink: a trashcan for all three that, in the fiction, turns everything into heat and blows it out of a brick chimney.

Milestone: adds the item "Sink and Chimney" to M3 (Manufacturing Tier 1 completion), proposed after M5 at the user's request like the pumps. PLAN.md is updated in the same change.

Revised after the first play test: the Sink became a 2x2x2 multiblock with one big fan, formed with the Configuration Carrot, and lost its side configuration.

## What Changes

- New Manufacturing block **Sink**: a brick casing block. Eight of them in a 2x2x2 cube form the Sink multiblock when the player clicks one face of the cube with the Configuration Carrot; that face becomes the front and shows one big fan behind a grate across all four front blocks. Unformed Sink blocks do nothing. Breaking any block of a formed Sink unforms it.
- The formed Sink accepts items, fluids and FE through `IItemHandler`, `IFluidHandler` and `IEnergyStorage` on every outer face except the front and voids everything it accepts. There is no side configuration.
- New Manufacturing block **Chimney**: brick blocks stacked straight on top of any of the four top corners of a formed Sink form a chimney column. The column joins visually and the top block carries a cap.
- **Chimney height sets the void rate.** The tallest column counts. With no chimney the Sink voids at its base rate per resource; every chimney block raises the rate; from a configured height on (example 5 blocks) the Sink voids without limit.
- While the Sink voided something recently it is active: the big fan spins (faster with a taller chimney) and smoke rises from the top of the tallest chimney column, or from the top of the cube without one.
- Right click on any formed block opens a small GUI with one trash slot that voids whatever is put in, the chimney height and the current rates. A filled bucket used on a formed Sink empties into it. Hand input is not rate limited.
- Rate tables per chimney height and the active time live in `data/servus_manufacturing/sink/sink.json` (examples: items 4, 8, 16, 32, 64 per second; fluid 50 to 800 mB per tick; energy 100 to 1,600 FE per tick; unlimited from 5 chimney blocks).
- Both blocks are hand crafted from bricks and iron (example: 8 bricks around iron bars make 8 Sink blocks).

## Capabilities

### New Capabilities
- `manufacturing/sink`: the Sink multiblock and Chimney blocks, forming and unforming, voiding over the three capabilities, rate by chimney height, active state with fan and smoke, trash slot GUI, buckets, crafting.

### Modified Capabilities
None.

## Non-goals

- Using the heat for anything (no heat output, no generator coupling).
- Filters (void only certain items), a void counter or statistics.
- Side configuration of the Sink.
- Other sizes than 2x2x2, chimneys wider than 1x1.
- Listing the Sink in the docs site game data export.
- Pollution or any world effect of the smoke.

## Impact

- `manufacturing`: `machine/sink` package (`SinkBlock`, `SinkBlockEntity`, `ChimneyBlock`, handlers, pure `VoidBudget`, `SinkRates` and `SinkLayout`), `SinkStatsManager` reload listener, `SinkMenu` and `SinkScreen`, `SinkRenderer` (big fan), registry entries, capability registration, datagen (blockstates, per part models, loot, recipes, lang), textures, `sink/sink.json`, GameTests.
- `PLAN.md`: section 2.1 and M3 gain the Sink.
- The Sink only uses NeoForge's `IFluidHandler` and does not depend on the fluid changes.
