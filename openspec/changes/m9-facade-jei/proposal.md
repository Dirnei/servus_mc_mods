# Proposal

## Why

Facades exist since M5, but players cannot find them in JEI. The Facade is in no creative tab, so JEI never lists it. Its block is stored on the stack without a JEI subtype rule, and the only example recipe uses Stone Bricks. A player who searches for "facade" finds nothing and has no hint that any full block works.

This is a play feel follow-up to PLAN.md M5 (facades and goggles) found during M9. It is added to the M9 list as item 1c, next to the Burner Pump follow-up.

## What Changes

- The Servus creative tab lists a few example facades (stone, stone bricks, oak planks, bricks, deepslate tiles), so JEI lists them too.
- JEI tells facades apart by their block, so each example is its own entry and shows its own name.
- JEI shows one example crafting recipe per listed facade instead of only the Stone Bricks one.
- Each facade gets a JEI information page saying that any full block without a block entity plus one nugget gives six facades of that block, and that the facade is used on a Utility Duct.
- PLAN.md gets M9 item 1c.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/facades`: add requirements for the facades listed in the creative tab and in JEI, with example recipes and an information page. Crafting, applying and rendering are unchanged.

## Non-goals

- Listing a facade for every full block in the game. The examples are enough to find the recipe, and the information page explains the rule.
- An item icon that shows the facade's block. The icon stays the generic facade texture for now; a block rendering icon can follow.
- Hollow facades, light blocking and goggles on the player model, still deferred from M5.
- Recipe viewers other than JEI.

## Impact

- `foundation` module: `registry/ServusFoundationCreativeTabs` or `logistics/ServusLogisticsEvents` (creative tab entries), `integration/jei/ServusFoundationJeiPlugin` (subtype interpreter, example recipes, information page), language datagen in `logistics/datagen/LogisticsDataParts` (information text).
- JEI stays a soft dependency: compile only API, and the plugin loads only when JEI is present.
- `PLAN.md` M9 list.
