# Design

## Context

`VeinGrowth.tick` (pure Java) advances a vein once per Hollow tick. While ripe it adds `work` ticks to `yieldTimer` and yields at `yieldTicks`. `work` comes from `GiftBoost.work()`, which turns the gift factor into whole ticks per tick and carries the fraction. Ripening takes `stepCost` from `PrincipleTanks`. Once ripe the tanks are never read again. `PrincipleDrip.plan` and `GnomeHollowBlockEntity.dripNeed()/receiveDrip()` refuse a ripe vein. The client flows the crack material at pace 1, or `GIFTED_PACE` 2 when `shownBoosted`.

## Goals / Non-Goals

**Goals:**
- One pure feed rule that JUnit covers: which principles count as fed, the speed factor, what one item uses.
- Reuse the existing tanks, bucket pouring and Dropper path. No new capability on the Hollow.

**Non-Goals:**
- Changing ripening, the store, gifts by the homunculus or the gnome entity's walking speed.

## Decisions

### Settles PLAN.md

- PLAN.md lists "gnome upkeep for speed" as gifts only. This change adds the principle upkeep. Wording in UI and book: "feed" (and "fed", "hungry") for principles, "gift" for the boost.
- Quality from feed goes to PLAN.md as an open point.

### Feed data

`feed` in the vein JSON is a map from principle id to `{amount, bonus}`:

```json
"feed": {
  "salt":    {"amount": 10, "bonus": 1.0},
  "mercury": {"amount": 4,  "bonus": 0.5},
  "sulfur":  {"amount": 2,  "bonus": 0.5}
}
```

`amount` is at least 1 and `bonus` at least 0. The field is optional, and missing means no feed. It is parsed in `VeinData`'s codec into a new record `VeinFeed(Map<Principle, Entry>)` stored on `Vein`. A map keyed by principle, not two `Principles` maps, keeps amount and bonus of one principle together and reads well in JSON. The principle key reuses the ids `PrincipleCodecs` already accepts.

Shipped data (examples, all bonuses summing to 2 so all three fed gives x3):

| Vein | yield_ticks | Salt mB / bonus | Mercury mB / bonus | Sulfur mB / bonus |
|---|---|---|---|---|
| coal | 600 | 4 / 1.0 | 1 / 0.25 | 4 / 0.75 |
| iron, copper | 1200 | 10 / 1.0 | 4 / 0.5 | 2 / 0.5 |
| quartz | 1800 | 10 / 0.5 | 15 / 1.0 | 5 / 0.5 |
| redstone | 1800 | 5 / 0.5 | 15 / 1.0 | 10 / 0.5 |
| gold | 2400 | 10 / 0.5 | 5 / 0.5 | 15 / 1.0 |
| diamond, emerald | 4800 | 80 / 0.75 | 50 / 0.75 | 30 / 0.5 |

Checked against the Dropper (25 mB per 20 ticks = 75 mB a minute per principle): diamond fed and gifted digs 1.5 a minute and needs 120 mB Salt a minute, so one Dropper keeps it fed at x3 (0.75 a minute, 60 mB) but not at x6. A gifted noble Hollow fed by one Dropper drops to a lower rate now and then. I take that trade-off: it rewards buckets on top or a stronger Dropper by datapack.

### Fed is read every tick, used when the item lands

`VeinFeed.fed(held)` returns the principles of the feed whose tank holds at least their amount. `VeinFeed.factor(held)` is 1 plus the sum of their bonuses. On `YIELD` the Hollow takes `VeinFeed.cost(held)`, the amounts of the principles fed at that moment.

Alternative: fix the fed set at the start of each dig and charge up front. I rejected it because a gnome that is fed during a long diamond dig would wait up to 4 minutes before speeding up. Reading every tick is safe: the Hollow's tanks never give fluid back, so a tank only falls when the gnome uses it. A principle fed at the end of a dig was fed since it arrived, and it pays its full amount even when it came in halfway. That rounds in the Hollow's favour and leaves no exploit.

### One pace for gift and feed

`GiftBoost.work()` becomes `GiftBoost.work(double feedFactor)`: it adds `factor() * feedFactor` to its carry and returns the whole ticks. One carry for both multipliers keeps fractions such as x2.5 exact over time. `VeinGrowth.tick` stays unchanged. It still takes `work`. Unripe veins ignore `work` as before. The tick passes `VeinFeed.factor` only when the vein is ripe and the store has room, otherwise 1.

### Dropper

`DripNeed` carries "wanted" principles instead of `stepCost`: the step cost while unripe and the feed amounts while ripe. `PrincipleDrip.plan(held, wanted, room, perDrip)` keeps its rule (only principles with wanted > 0), with the parameter renamed. `receiveDrip` uses the same wanted set. The Dropper keeps the Hollow's tanks topped up to their capacity. It does not stop at one dose, because the feed is a buffer and topping up is what a player expects.

### Client look

The Hollow syncs a whole `feedPace` (1 to 3, the feed factor rounded) next to `shownBoosted`. The update is sent only when the rounded value changes. The renderer uses `pace = feedPace * (shownBoosted ? GIFTED_PACE : 1)`. The pace stays a whole number, so the phase still wraps without a jump. The fed set only changes when an item lands or principles are poured or dripped, so the sync stays rare.

### Jade

The server data adds per principle a `Fed` flag for principles in the feed (only for a ripe, bound vein), and the current ticks per item (`yieldTicks / (feedFactor * giftFactor)`). The tooltip appends "fed" or "hungry" to the existing principle lines of feed principles and adds one line "One item per %s". The text uses the existing `clockText`. Jade API as already used in `ServusAlchemyJadePlugin` (`IServerDataProvider<BlockAccessor>`, `ITooltip`), no new API.

### NeoForge APIs

None new. The vein codec uses Mojang `Codec`/`RecordCodecBuilder` and `Codec.unboundedMap` with the existing principle key codec. Tanks are the existing `PrincipleTanks` on NeoForge `FluidStack`. The client sync uses `getUpdateTag`/`sendBlockUpdated` as today.

### Pure Java vs Minecraft

- Pure (JUnit): `VeinFeed` (fed, factor, cost), `GiftBoost.work(feedFactor)` carry, `PrincipleDrip.plan` with wanted feed amounts, vein file test (every shipped vein has a feed of all three principles, bonuses summing to 2, the minimum yield ticks).
- Minecraft (GameTest): Hollow digs at the fed rate and uses principles, unfed digs at base, full store uses nothing, Dropper drips into a ripe vein, gift times feed.
- Manual: the crack flow speed and the Jade lines under the sight.

## Risks / Trade-offs

- [Existing worlds dig much slower overnight] → Changelog "Changed" line. The journal page tells players what the gnome eats. No migration, since the feed is just new data.
- [Datapack veins from other packs have no feed and a fast `yield_ticks`] → They keep their old behavior. That is the intended default for a vein without a feed.
- [One Dropper cannot keep a gifted diamond Hollow at x6] → Accepted (see Feed data). The drip amount is data.
- [GameTests that set `yieldTimer` to `IRON_YIELD_TICKS - 1` rely on the old 200] → They read `yieldTicks` from the loaded vein. Tests that hard-code 200 get updated.
