# Design

## Context

The Calciner (`CalcinerBlockEntity`) is a `BurnerMachineBlockEntity` from Foundation with two inventory slots, input and fuel. The base class burns one fuel item when `burnTime` reaches 0 and work is available, and offers the fuel slot as upkeep through `upkeep()`. The cycle time is the recipe's `time` (`workTicks()`), not the machine stats. The Firebox is a `CalcinerPartBlock` with role `FIREBOX`; it takes fuel by hand in `useItemOn` and through `CalcinerPorts.items` as `fuelPort` (`SlotPort.insertOnly` with `burns`). `MachineBlockEntity.preRemoveSideEffects` drops every slot when the Calciner block goes. `CalcinerRenderer` already draws the input item in the crucible from a synced `shownInput`.

Ignis Perpetuus is a plain `QuoteItem` with no burn time (`ServusAlchemyItems.IGNIS_PERPETUUS`), and the `ignisIsNotAFuel` GameTest keeps it out of furnaces.

## Goals / Non-Goals

**Goals:**
- One Ignis Perpetuus held by the Calciner replaces fuel and upkeep and shortens the cycle by a datapack factor.
- No change for any other burner machine.

**Non-Goals:**
- A general "eternal fire" item tag or upgrade for every burner machine. The Foundation hook allows it later, but no other machine uses it here.

## Decisions

### Settles `alchemy_plan.md`, Open, Play: "Is Ignis Perpetuus also an endless fuel?"
Yes, in the Calciner only, set into the Firebox, and it makes it faster. It stays no furnace fuel. The question is removed from `alchemy_plan.md`, and the "It is not a fuel" line in the docs is updated.

### The Ignis lives in a third inventory slot of the Calciner
`SLOT_IGNIS = 2`, the block entity is created with 3 slots. No port exposes it: `inputPort`, `fuelPort` and `ashPort` cover only their own slots, and `isInputSlot`/`isFuelSlot` do not include it. Saving, loading and dropping on break come free from `MachineBlockEntity` (`dropContents` drops every slot).
Alternative: a boolean flag. Rejected because it needs its own save field and its own drop code, and a slot keeps the item's components (a renamed Ignis comes back renamed).

### A Foundation hook for a fire that never goes out
`BurnerMachineBlockEntity` gets `protected boolean eternalFire()`, `false` by default. When it is true, `tick()` treats the machine as burning without calling `consumeFuel()` and without counting `burnTime` down, and `upkeep()` returns `null`. The Calciner overrides it as "the Ignis slot is not empty". `getData` reports a full burn while it is true, as `SalamanderForgeBlockEntity` does.
Alternative: refill `burnTime` from the Calciner every tick. Rejected because upkeep would still be reported from the base class and the trick would hide in a subclass.

### Speed factor as Alchemy datapack data
New loader `calciner/EternalFireData` (a `SimpleJsonResourceReloadListener` like `OldGnomeData`) reads `data/<ns>/servus_alchemy/eternal_fire/calciner.json` with one field `speed` (int, 1 to 64, default 1 without the file). Shipped file: `{ "speed": 2 }`. Registered in `AlchemyEvents` with `AddServerReloadListenersEvent` like the other loaders.
The pure helper `calciner/EternalFireSpeed.cycleTicks(int recipeTicks, int speed)` returns `max(1, recipeTicks / max(1, speed))` and is covered by `EternalFireSpeedTest` (JUnit, no Minecraft). `CalcinerBlockEntity.workTicks()` uses it while `eternalFire()` is true.
Alternative: a new field in Foundation's `MachineStats`. Rejected: Foundation's stats should not carry an Alchemy item's effect, and the Ignis is Alchemy's.

### Interaction at the Firebox
- `CalcinerPartBlock.useItemOn`: on a formed Firebox, Ignis Perpetuus in hand calls `CalcinerBlockEntity.setIgnis(player, hand)`; this moves one Ignis into the slot, gives any fuel in the fuel slot to the player with `PlayerItems.give`, and syncs. With an Ignis already set it returns `SUCCESS` without change, so the item is not used on anything else.
- `burns(stack)` returns false while the Ignis is set, so `insertFuel`, the hand path and `fuelPort` all refuse fuel.
- `useWithoutItem` on a formed Firebox while the player sneaks gives the Ignis back (`takeIgnis`).
- `CalcinerPartBlock.affectNeighborsAfterRemoval` for the Firebox takes the Ignis out of the Calciner and drops it with `Block.popResource` before `CalcinerStructure.broken` unforms. The formed state still names the controller then, so `CalcinerPorts.calciner` finds it.

### Showing the Ignis
`getUpdateTag` adds `Ignis` (true or false), and `setIgnis`/`takeIgnis` send `sendBlockUpdated`. `CalcinerRenderer` draws the Ignis Perpetuus item standing in the Firebox, one block below the Calciner, behind the grate, at full brightness (`LightCoordsUtil.FULL_BRIGHT`, as `HollowRevealRenderer` uses) so it glows while idle. Its quad is a flat item standing 2 px behind the Firebox's front face and inset 0.05 px from the grate bars, so no face shares a plane with the formed model; the play check looks at it from the front and the sides.

### Jade
`CalcinerProvider` writes `Ignis` into the server data; the client shows a line `jade.servus_alchemy.calciner.eternal_fire` ("Fire: Ignis Perpetuus") instead of the fuel line while it is set. Lang comes from `ServusAlchemyLanguageProvider`.

### Journal
`WorkshopCategory`: the Ignis Perpetuus spotlight on the Salamander entry with a text page "The Eternal Flame" after it (a spotlight page is not split, so it stays short) and the fire text of the Calciner entry say that set into a Calciner's Firebox it burns forever and calcines faster, and that a sneak with an empty hand takes it back. The text says "faster", not the number, because the number lives in the datapack.

### APIs used
- `SimpleJsonResourceReloadListener`, `FileToIdConverter`, `AddServerReloadListenersEvent` (already used by `OldGnomeData` and `AlchemyEvents`, NeoForge 26.1).
- `Level.invalidateCapabilities(BlockPos)` (NeoForge `IBlockGetterExtension`/`ILevelExtension`, invalidates `BlockCapabilityCache` entries).
- `Block.popResource(Level, BlockPos, ItemStack)` (vanilla, used across the repo for drops).
- `Block#useItemOn`, `Block#useWithoutItem`, `Block#affectNeighborsAfterRemoval` (already overridden in `CalcinerPartBlock`).
- `RenderSubmit.item` with `ItemDisplayContext.FIXED` (already used by `CalcinerRenderer.renderInput`).

### Pure Java and GameTests
- Pure Java, JUnit: `EternalFireSpeed.cycleTicks` (`EternalFireSpeedTest`).
- GameTests in `CalcinerGameTests`: set by hand and coal returned, only one, not at the mouth, no fuel needed, coal refused by hand and by port, no upkeep, twice as fast (40 vs 80 ticks with the shipped data), take back, kept while unformed, Firebox break drops it, Calciner break drops it.
- Manual play check: the glow in the grate from several angles, idle and working, no flicker.

## Risks / Trade-offs

- [Capability caches keep the old upkeep holder of the Firebox] → `setIgnis` and `takeIgnis` call `Level.invalidateCapabilities` on the Firebox position, so cached lookups ask again; the GameTest "no upkeep" checks that the Firebox reports no upkeep after the Ignis is set.
- [Old saves have a 2-slot Calciner] → the project is unreleased; the slot list loads with the extra slot empty anyway.
- [Fuel in the slot while unformed] → setting the Ignis needs a formed Firebox, so the fuel always goes back to a player standing at it.
- [The m12-salamander spec wording "building part only"] → flagged in the proposal; its furnace scenario is unchanged.
