# Design

## Context

Builds on `m5-duct-endpoint-gui` (flags, redstone setting, On/Off, layout) and `m5-duct-fluid-filter` (pure `FluidFilter`, fluid filter slots, tiered `FluidRouting`, filtered sources and sinks). Apply after both.

Item filters are nine ghost stacks per side in `ItemSlots`, matched with `ItemStack.isSameItem` in `UtilityDuctBlockEntity.filterAllows`. `ItemChannelNetwork.pull` simulates `extractItem(slot, perPull)` per slot and routes the first stack that passes; `capacity(endpoint, origin, stack)` simulates inserting into a target to plan the round robin. `FluidChannelNetwork` builds `Source` offers from a drain simulation and `Sink`s that fill.

Fluid handlers are NeoForge `ResourceHandler<FluidResource>` (`size()`, `getResource(i)`, `getAmountAsLong(i)`, as used in `SinkBlockEntity`); item handlers are wrapped in `ItemView` with slots.

## Goals / Non-Goals

**Goals:**
- One number per filter entry whose meaning follows the side's direction.
- No per tick scans beyond the endpoints the network already touches.

**Non-Goals:**
- Overflow Valve block, warehouse excess, amounts without entries, ratios.

## Decisions

### D1 One amount, meaning by direction
An entry's amount is a keep amount when its side gives and a fill up to amount when its side receives. For fluid sides with In and Out the role is the one `FluidChannelNetwork.tickChannel` assigns that tick (sink or source). Item in and out sides only receive routed items (items are only pulled from pull in sides), so for them the amount is always up to. This avoids two numbers per entry and matches how players describe it ("keep 64 here", "at most 16 there").

### D2 Pure `KeepAmounts`
`logistics/duct/KeepAmounts`:
- `giveAllowance(long held, int keep, int offered)` = `offered` when `keep <= 0`, else `clamp(held - keep, 0, offered)`;
- `receiveAllowance(long held, int upTo, int offered)` = `offered` when `upTo <= 0`, else `clamp(upTo - held, 0, offered)`.
JUnit `KeepAmountsTest` covers the spec's table plus overflow of large `held` values.

### D3 Storage
Item amounts: `itemAmounts[side][slot]` (int, 0 = none) beside `ItemSlots filters[side]`, saved under `"FilterAmounts"` (side name to int array, only non-zero sides), cleared when the slot is cleared or set to another item. Fluid amounts: `FluidFilter` gets `amount(slot)` and `setAmount(slot, int)` and clears the amount with the slot; saved with the fluid filter. Both are in the update tag and survive dismantling. `amountFor(side, stack)` and `amountFor(side, fluidId)` return the first matching entry's amount.

Max amount: items 1,000,000, fluids `Integer.MAX_VALUE` mB, clamped on the server.

### D4 Counting what a block holds
- Items: `heldCount(view, stack)` sums `getStackInSlot(i)` of every slot with `isSameItem`. Computed once per pull for the candidate stack and once per `capacity` call for a target with an up to amount; targets and sources without amounts skip the count, so the common case costs nothing new.
- Fluids: `heldAmount(handler, fluid)` sums `getAmountAsLong(i)` over `size()` where `getResource(i)` is that fluid. Computed when building a source offer (cap the offer with `giveAllowance`) and inside the `Sink.fill` wrapper (cap with `receiveAllowance` from the live amount, so repeated fills within one routing pass see the rising level).

`ItemChannelNetwork.pull` caps the simulated extract with `giveAllowance` before routing and extracts only that. `capacity` caps the insert simulation with `receiveAllowance`. `FluidChannelNetwork.fill` (pushed in) caps each sink the same way.

### D5 Editing
Right click on a set filter slot opens an amount popup (`client/PopupPanel` with the Foundation `NumberStepper`), steps 1, 16, 64 for items and 100, 1,000, 8,000 for fluids, plus Clear and Save. The label is "Keep", "Up to" or "Keep / up to" from the side's mode on that line. Save sends `SetDuctFilterAmountPayload(line, slot, amount)`; the server checks the open menu, the line, a set slot, and clamps. The slot overlay draws the amount in the stack count position, fluids formatted with `k` above 1,000 (8,000 as "8k", 1,500 as "1.5k").

Right click opens the popup because it is the frequent action; clearing moves to shift right click, which was free (shift left click is shift click).

### D6 PLAN.md
Remove the overflow valve on the duct from "Open: Foundation" (added by `m5-duct-endpoint-gui`) and keep the Overflow Valve block and warehouse excess lines.

## Settled PLAN.md decisions

The open point "duct overflow" is settled: amounts live on filter entries, one number whose meaning follows the side's direction, counted over the whole block.

## APIs

- `ResourceHandler.size()`, `getResource(int)`, `getAmountAsLong(int)` (NeoForge 26.1 transfer API, used in `SinkBlockEntity`).
- `ItemStack.isSameItem`, the existing `ItemView` slot access.
- Foundation `NumberStepper` and `PopupPanel`.

## Pure vs Minecraft

- Pure, JUnit: `KeepAmounts`, `FluidFilter` amounts (set, clear with the slot, first match wins) in `FluidFilterTest`.
- Minecraft, GameTest: keep 64 saplings with a composter, tank to Sink at 8,000 mB keep, coal up to 16 with overflow to a lower priority chest, refill after use, amounts over several slots, save and load.
- Manual: popup, overlay text, labels per mode.

## Risks / Trade-offs

- [Counting large inventories (drawers, storage mods) on every pull] -> only sides with amounts count, at most once per pull interval per side; acceptable, revisit if the potato shows slow ducts.
- [Fluid side with In and Out switching role] -> the role is decided per tick before amounts apply; a side that received fluid stays a sink as today, so its amount stays an up to amount.
- [Two entries for one item] -> first one wins, stated in the spec.

## Decisions made while applying

- An item filter slot remembers which item its amount was set for; when the slot is cleared or shows another item, the amount drops, so setting the same item again starts without one.
- Fluid amounts are saved per side under `"FluidFilterAmounts"` beside `"FluidFilters"`, item amounts under `"FilterAmounts"`.
- The overflow to a Sink GameTest uses a second Fluid Tank as the receiver, since forming a Sink multiblock is not needed to show the keep amount. A separate GameTest covers a fluid fill up to amount.
- The 300 sapling GameTest uses 100 saplings in two stacks with keep 64 (36 move): the duct pulls 8 items every 8 ticks, so 236 would take well over a minute of game time. The three stack test keeps 100 of 192 cobblestone (92 move).
- An item side gives only with Pull, so an item side with Out and without Pull says "Up to", one with Pull and without Out "Keep", and one with both, like a fluid side with In and Out, "Keep / up to". A side that neither gives nor receives also shows "Keep / up to".
- The amount popup is its own small `logistics/client/AmountPopup` over the GUI with the Foundation `NumberStepper` (from 0) and Clear and Save drawn like the channel popup buttons. Enter saves, Escape closes.
- The tooltip of a set filter slot adds a grey "Right click: amount, shift right click: clear" hint, so players can find the popup.
- The potato's filtered side line carries the count of entries with an amount right after the filter entry count.
- After the play check the amount popup was too small for fluid amounts. `NumberStepper` now measures its labels with the font: a step button is as wide as its widest plus or minus label with 3 px each side (never under 24 px), the field fits the largest value (never under 48 px), and `width()` covers both. `AmountPopup` measures the stepper, Clear and Save and the title and grows from 110 px up to the GUI width, cutting only a long title. Fluid steps 100, 1,000 and 8,000 with a maximum of 2,147,483,647 mB fit; the Storage Terminal's steppers keep their size.
