# Design

## Context

`GogglesItem.reveals(player)` (goggles worn or carrot held) drives three things: `DuctLoadSync` (server sends load levels of nearby data ducts twice a second while true), `DuctOverlayRenderer` (draws them) and `PanelReveal` (facade panels half transparent). `SideMarkerRenderer.configuring()` has its own copy of the same rule for side markers. Bare hand clicks already read a little: a data duct shows `DataSummary` in the action bar, a Fluid Tank shows its fill line (`FluidTankBlock.describe`). `UtilityDuctBlock.useItemOn` passes carrot, line and facade items through to the item; everything else falls to `useWithoutItem`. `FluidTankBlock.useItemOn` passes any non fluid container item. Line networks are found with `NetworkSet.networkAt(pos)`; `ServusNetwork.size()` is the run size. `ArmHit.hit` tells which arm or the centre a hit landed on.

Applied after `m5-duct-filter-picker` and `m5-named-channels`; the duct report reads channel names and colours from the named channel list (`ChannelLookup` on the server).

## Goals / Non-Goals

**Goals:**
- One rule class that every reveal user calls, with three clearly named rules.
- Reports from data that already exists; no new counters in networks.

**Non-Goals:**
- No change to the bare hand summaries; they stay as quick reads.

## Decisions

### D1: Three reveal rules in one place

`item/Reveal` replaces `GogglesItem.reveals`: `configuring(player)` (goggles or carrot), `analysing(player)` (goggles or potato), `seesThroughPanels(player)` (either). `SideMarkerRenderer.configuring()` delegates to `Reveal.configuring`, `DuctLoadSync` and `DuctOverlayRenderer` use `analysing`, `PanelReveal` uses `seesThroughPanels`. `GogglesItem.isWearing` stays. Alternative: keep one rule and let the potato join it. Rejected, the carrot would keep the overlay, which is what the user wants gone.

### D2: The potato is a plain item that acts in `useOn`

`DataPotatoItem` (`foundation/item/`), stacks to 1, registered in `ServusFoundationItems` and the Servus creative tab. `useOn` runs only on the server: a duct gives the duct report, a Fluid Tank the tank report, anything else passes. Each report line goes out with `player.sendSystemMessage`. `UtilityDuctBlock.useItemOn` and `FluidTankBlock.useItemOn` add the potato to the items they pass through, so the click reaches the item instead of `useWithoutItem`. The potato never configures or dismantles, also when sneaking.

### D3: Duct report

`DuctReport` (`foundation/logistics/duct/`) builds the lines from the duct block entity and the line networks:

| Line | Arm click | Centre click |
|---|---|---|
| Item | mode, channel name in its colour, priority, run size, `in` and `out` on that channel | run size |
| Power, Fluid | mode, channel, run size, `in` and `out` on that channel | run size |
| Data | tier (cable item name), bandwidth items/s, load of the last second (`DataNetwork.load`) | the same |

Example:
```
Utility Duct, north side
Item Pipe: Push, "Iron ore", priority 2, 14 ducts, 2 in / 3 out
Power Cable: In and out, Default, 9 ducts, 1 in / 4 out
Data Cable: Fast Data Cable, 64 items/s, 45 used
```
The arm is found with `ArmHit.hit` from the click location like the carrot does. `in` and `out` come from the pure `EndpointTally` (D4) over the side's channel. Its input is `LineNetwork.blockSides()`: the channel and mode of every member duct side whose arm of the line ends at a block (`hasArm` and `isEndpoint`, read live like `channelUses()`, which now counts over the same list). The cached endpoints are not used, because one exists for every enabled side, air and duct sides included. Lines go out with `player.sendSystemMessage`; `DuctReport.of(duct, side)` returns them as a list so GameTests read them directly, and `PlayerClicks` can collect the chat packets of a full click.

### D4: Pure endpoint tally

`EndpointTally` (`foundation/logistics/network/`, no Minecraft classes): given the channel to count and a list of (channel, `SideMode`) pairs, returns `in`, the sides where the run takes from a block (pull in, in and out), and `out`, the sides where the run delivers to a block (push out, in and out); an in and out side counts on both, disabled sides and other channels on neither. JUnit `EndpointTallyTest`.

### D5: Tank report

`FluidTankBlock.describe(content, capacity)` gives the fill line; the report adds the size from the tank's `TankShape` box (`TankNetwork.boxAt`) as `w x h x d`. One or two chat lines.

### D6: Item look and recipe

Recipe (datagen, `ServusFoundationRecipeProvider`): shaped, redstone on top, Data Cable in the middle, potato at the bottom, like the carrot's column (redstone, iron, carrot). Texture `textures/item/data_potato.png`, 16 by 16: a potato with a copper probe and a small green readout, made by `tools/potato-texture/gen.py` (Python 3, standard library) so it can be redrawn like the other generated textures. Item model by `basicItem`, lang "Data Potato" plus a tooltip line.

### APIs

- Minecraft: `Item.useOn(UseOnContext)`, `Player.sendSystemMessage`, `Component.withStyle` with `TextColor.fromRgb` for the channel colour.
- NeoForge: none new; the overlay sync keeps `PacketDistributor.sendToPlayer`.

### Pure logic and tests

- JUnit: `EndpointTallyTest` (`inAndOutCountsBoth`, `pullInCountsIn`, `pushOutCountsOut`, `disabledIgnored`, `otherChannelIgnored`). The reveal rules need a player, so GameTests cover them.
- GameTests (`foundation/.../logistics/gametest/`): `potatoReportsItemArm`, `potatoReportsDataLine`, `potatoReportsTank`, `potatoChangesNothing`, `revealRules` (mock players with carrot, potato, goggles, pickaxe).
- In game: overlay with potato and not with carrot, panels for all three, side markers with carrot only.

### PLAN.md

No open decision covers this. Archiving removes the `m5-data-potato` open point and adds "Deeper inspection with the Data Potato: throughput per channel, why items are stuck, machine and storage reports" under "Open: Foundation".

## Risks / Trade-offs

- [Players used to seeing the load with the carrot] → the potato's tooltip and the goggles still show it.
- [Chat reports scroll away] → kept to one line per duct line; deeper views are a later change.
- [Run size and tallies only count loaded parts of a run] → the header line says the counts cover the loaded part.
