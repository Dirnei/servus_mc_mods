## Why

`alchemy_plan.md` asks for gnome upkeep for speed: a gnome that gets something it likes works faster for a while, and without gifts it keeps the base rate. Today a bound gnome only ever works at its vein's yield time. The later Receptaculum (`m12-receptaculum`) is meant to bring gifts automatically, so the gift itself and a seam for that delivery come first.

## What Changes

- New datapack data `data/<namespace>/servus_alchemy/gnome_gift/<name>.json`: liked items and item tags, a speed factor and a duration. Shipped: cookie, bread, sweet berries and glow berries, x2 for 10 minutes.
- A bound gnome that takes a gift digs faster by the gift's factor until the duration runs out; a new gift refreshes the boost. Without a gift it works at the base rate. Gifts are never required.
- By hand for now: a player with Elemental Sight uses a liked item on a bound Hollow; one item is used up. A disliked item is refused with an action bar hint; without the sight nothing happens, as with stone.
- An API seam on `GnomeHollowBlockEntity` (`offerGift`, `boostTicksLeft`) so the Receptaculum can deliver gifts later.
- The boost is saved with the Hollow and counts down while the Hollow ticks.
- Jade under the sight shows the boost and its remaining time; the revealed gnome spins and bobs faster while boosted.
- Journal Workshop entry "Gnome Hollow" gets a gifts page; docs get a gnome gift datapack page and a line in the Alchemy page.
- Pure `GiftBoost` timing with JUnit tests; GameTests for the hand gift.

This change builds on the applied but unarchived `m12-hidden-hollow` (the Hollow as disguised stone with an internal store); its delta for `alchemy/gnome-hollow` is the base this change reads. This change adds a new capability and does not modify that spec's requirements.

## Capabilities

### New Capabilities
- `alchemy/gnome-gifts`: gift data, the speed boost of a bound gnome, giving by hand under Elemental Sight, the delivery seam, saving and Jade.

### Modified Capabilities
None.

## Impact

- `alchemy/.../hollow/`: new `GiftBoost` (pure), `GnomeGiftData` (reload listener), changes in `VeinGrowth` (work per tick), `GnomeHollowBlockEntity`, `GnomeHollowBlock`.
- `AlchemyEvents` registers the new listener.
- `HollowRevealRenderer`, `ServusAlchemyJadePlugin`, `ServusAlchemyLanguageProvider`, `WorkshopCategory`.
- Data: `data/servus_alchemy/servus_alchemy/gnome_gift/treats.json`.
- Docs: `docs/src/content/docs/mods/alchemy.md`, new `reference/datapack/gnome-gifts.md`.
- Tests: `GiftBoostTest`, `VeinGrowthTest`, `HollowGameTests`.
