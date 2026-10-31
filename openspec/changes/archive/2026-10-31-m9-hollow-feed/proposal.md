# Proposal

## Why

A ripe Gnome Hollow digs its ore forever without using anything. An iron vein gives 360 Raw Iron an hour and a diamond vein 60 Diamonds an hour for free, twice that with a cookie, and every extra Hollow adds the same again. The Hollows need an upkeep, and the principles are the natural one: Tria Prima leaves a surplus of some principle all the time, and today only the Incinerator takes it.

This belongs to PLAN.md M9 (Alchemy, Nigredo and Albedo), item Gnome Hollow.

## What Changes

- A ripe Hollow is fed principles while its gnome works. Each vein names its feed in its JSON: for each principle, the amount one dug item uses and the bonus it gives.
- Feeding is a bonus, never a gate. An unfed gnome still digs at the vein's base rate. Each principle whose tank holds at least its amount counts as fed and adds its bonus: the gnome digs at base rate times (1 + the sum of the fed bonuses). Every dug item uses up the amount of each fed principle.
- Each shipped vein gets its own mix (iron mostly Salt, gold mostly Sulfur, quartz and redstone mostly Mercury, diamond and emerald all three in large doses). With all three fed it digs three times as fast.
- A gift stays a separate multiplier on top: a fed and gifted gnome digs six times the base rate.
- The base rates of the shipped veins become much slower, so that unfed is less than today and fed plus gift is about today's rate (example: iron 1 per 60 s unfed, diamond 1 per 4 min unfed and 90 an hour fed with a gift).
- The Principle Dropper also drips into a ripe Hollow: only the principles that vein's feed names.
- Under Elemental Sight the material in the cracks flows faster the better the gnome is fed. Jade shows for each principle of the feed whether it is fed and how long the gnome takes per item.
- The journal's Gnome Hollow and Principle Dropper pages say what the gnome eats.
- PLAN.md gets an open point for feed quality (a principle that gives a chance of a second ore instead of speed).

## Non-goals

- Quality from feeding (a chance of more than one item per dig). Recorded as an open point.
- Overdosing: too much of one principle never hurts.
- Feeding by pipe or capability into the Hollow. It still exposes none; the Dropper and buckets are the ways in.
- A book table of each vein's diet, revealed per team. Can follow later like the calcination table.
- Gifts by the homunculus or the gnome's walking speed: unchanged.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `alchemy/gnome-hollow`: veins carry a feed; the working gnome is fed principles and digs faster with them; slower base rates; the sight look and Jade show the feed.
- `alchemy/gnome-gifts`: a gift multiplies the fed rate, not only the base rate.
- `alchemy/principle-dropper`: drips into ripe veins the principles their feed names.

## Impact

- Code: `alchemy/hollow/VeinData`, `VeinGrowth`, a new pure `VeinFeed`, `GiftBoost`, `GnomeHollowBlockEntity`, `PrincipleDrip`, `PrincipleDropperBlockEntity`, `client/sight/HollowRevealRenderer`, Jade plugin, language provider, journal book pages.
- Data: all eight shipped vein JSON files (new `feed`, new `yield_ticks`).
- Tests: JUnit for feed and drip logic, vein file test; Hollow and Dropper GameTests.
- Behavior change for existing worlds: ripe Hollows dig slower until fed. No migration; a "Changed" line in the Alchemy changelog.
