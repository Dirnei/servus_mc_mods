# Design

## Context

See proposal.md for the why. After `m12-hidden-hollow` (applied, not archived) `GnomeHollowBlockEntity` is a plain `BlockEntity` with its own server ticker, `PrincipleTanks`, pure `VeinGrowth.State`, pure `HollowStore` and team ownership. `VeinGrowth.tick(vein, state, held, outputHasRoom)` advances `yieldTimer` by one per tick and yields at `yieldTicks`. `GnomeHollowBlock.useItemOn` is gated by `ElementalSight.active(player)` and tries, in order: Salt Crystal binds, a fluid container pours, anything else seeds. Veins load from `data/<ns>/servus_alchemy/vein/` through `VeinData`, a `SimpleJsonResourceReloadListener` registered in `AlchemyEvents`. The client reveal renderer draws a spinning, bobbing red cap for a bound gnome from the update tag. The Jade `HollowProvider` sends Hollow data only to a viewer with the sight.

## Goals / Non-Goals

**Goals:**
- A gift boost that is data driven, pure in its timing, given by hand under the sight, saved, and visible in Jade under the sight.
- A seam the Receptaculum can call without knowing about players.

**Non-Goals:**
- Automatic delivery (`m12-receptaculum`), named gnomes or moods (open story point), gifts that speed up ripening.

## Decisions

### D1 Pure `GiftBoost`
`hollow/GiftBoost` (no Minecraft imports) holds `factor` (double), `ticksLeft` (int) and a fractional `carry`. `give(factor, ticks)` replaces any running boost (refresh, no stacking). `work()` is called once per Hollow tick: it counts down `ticksLeft` while boosted, adds the current factor (1 when not boosted) to `carry` and returns the whole work units, so a factor of 1.5 alternates 1 and 2. `active()`, `factor()`, `ticksLeft()`, `load(factor, ticks)`. JUnit `GiftBoostTest`.
- Alternative: shorten the yield time while boosted. Rejected: a fractional factor would round per item, and a change in the middle of a yield would jump the timer.

### D2 `VeinGrowth` takes work per tick
A new overload `tick(vein, state, held, outputHasRoom, work)` adds `work` to `yieldTimer` instead of 1, yields at most one item per tick and keeps the remainder below `yieldTicks`. Ripening ignores `work`: the gnome works the vein, the vein ripens by itself. The old four argument `tick` delegates with work 1.

### D3 Gift data
`hollow/GnomeGiftData` is a `SimpleJsonResourceReloadListener` over `servus_alchemy/gnome_gift`, registered in `AlchemyEvents` next to `VeinData`. Format:
```json
{ "items": ["minecraft:cookie"], "tags": ["c:foods/berry"], "factor": 2.0, "duration_ticks": 12000 }
```
`items` uses `BuiltInRegistries.ITEM.byNameCodec()` and `tags` uses `TagKey.codec(Registries.ITEM)`, both of which parse with plain `JsonOps` (an `Ingredient` holder set would need registry ops). Both lists are optional, `factor` is `Codec.doubleRange(1, 100)`, `duration_ticks` at least 1. `forItem(stack)` returns the first gift by id that holds the item. Shipped: `data/servus_alchemy/servus_alchemy/gnome_gift/treats.json` with cookie, bread, sweet berries and glow berries, factor 2, 12,000 ticks.

### D4 The seam: `offerGift`
`GnomeHollowBlockEntity.offerGift(ItemStack)` returns `Gift { TAKEN, DISLIKED, NO_GNOME }`, never shrinks the stack (the caller does on `TAKEN`, as with `seed` and `bind`), starts the boost, saves and syncs the boosted flag to clients. `NO_GNOME` when no gnome is bound (an unbound or empty Hollow cannot be pleased). `boostTicksLeft()` and `boostFactor()` let the Receptaculum wait until a boost runs low. Server side only, like every other Hollow mutation.

### D5 Hand gift in `useItemOn`
Under the sight, after the Salt Crystal and fluid branches: on a Hollow with a vein, a liked item gives a gift (`TAKEN` shrinks one outside creative, then `gift_taken`; `NO_GNOME` shows `gift_no_gnome`), and on a bound Hollow any other item shows `gift_disliked`. On an unbound Hollow a non-gift item still goes to `seed` (hint `already_seeded`), and on an empty Hollow everything goes to `seed` as before, so cookies on an empty Hollow say "does not take root". Without the sight the method already returns `PASS` before any of this.

### D6 Ticking, saving and the client cue
`tick()` calls `boost.work()` every tick (before the vein check, so the countdown runs whenever the Hollow ticks) and passes the work to `VeinGrowth.tick`. When the boost ends it syncs once so the client stops the faster gnome. Saved as `GiftFactor` (double) and `GiftTicks` (int). The update tag carries `Boosted`; `HollowRevealRenderer` runs the gnome's spin and bob at twice the speed while it is set (integer multiple, so the 400 tick phase wraps without a jump). No particles: server particles would be seen by everyone and betray the stone.

### D7 Jade
`HollowProvider.appendServerData` adds `GiftFactor` and `GiftTicks` when boosted; the client adds a line `Gift: x2, 8:00 left` (`jade.servus_alchemy.gnome_hollow.gift`), with the time as minutes and seconds of game ticks. The Stone disguise is unchanged.

## Risks / Trade-offs

- [The boost runs down while the store is full, so a gift on a full Hollow is partly wasted] → Simple rule, and the hand player sees the store in Jade; the Receptaculum empties the store anyway.
- [A weaker gift replaces a stronger running one] → Only matters when a pack ships gifts with different factors; noted below.
- [Item names in the journal text are written out, not read from data] → Journal texts are datagen; a pack that changes gifts changes the docs page, not the journal.

## Assumptions to confirm with the user

- Gifts: cookie, bread, sweet berries and glow berries, x2 for 10 minutes (12,000 ticks), in one shipped file `treats.json`.
- A new gift refreshes: the boost restarts with the new gift's factor and full duration; boosts never stack, and a weaker gift replaces a stronger one.
- The boost counts down every tick the Hollow ticks (its chunk loaded), also while the store is full; not by world time, so an unloaded Hollow keeps its boost.
- Gifts speed up only digging by a bound gnome, not ripening; a liked item on an unbound Hollow is refused with a hint.
- On a bound Hollow every item that is no gift (also raw ore) shows "the gnome does not want this" instead of "a vein already grows here".
- The client cue is the revealed gnome spinning and bobbing twice as fast; no particles.
- Hand gifts need Elemental Sight like every other Hollow interaction; without it nothing happens and nothing is said.
