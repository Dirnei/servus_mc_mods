# Proposal: m9-gnome-hollow

PLAN.md milestone M9 (Manufacturing becomes alchemy), item 6: `m9-gnome-hollow`.

## Why

PLAN.md 2.1: no world generation; gnomes grow ore veins instead. In Paracelsus' time miners believed metals grow in the earth like plants. A Gnome Hollow is seeded with a sample of an ore, ripens while it is fed principles, and once a gnome is bound to it, it yields that ore at a fixed rate forever. The vein and the gnome are only seen under Elemental Sight.

## What Changes

- **Gnome Hollow**: a block that looks like plain stone. It has a sample slot, an output slot and one tank per principle, belongs to the team of the player who placed it, and is made on an Albedo Athanor.
- **Veins are data**: `data/<ns>/servus_manufacturing/vein/<name>.json` with the sample item tags that seed it, the yield, the journal chapter a team must have finished, the ripening (steps, ticks per step, principles per step) and the yield rate. Shipped: iron and copper (after Villach), matched by `c:` tags, so ores of other mods can be grown by adding a vein file.
- **Seeding**: putting a matching sample into the sample slot seeds the vein and uses the sample. A sample whose vein's chapter the team has not finished is refused.
- **Ripening**: every step the Hollow takes the step's principles from its tanks and the vein grows one step; without them it waits.
- **Binding**: a ripe vein needs a gnome. A player with Elemental Sight binds one by using a Salt Crystal on the Hollow as an offering.
- **Working**: a bound gnome yields the vein's item at the fixed rate into the output slot, pushed out by the usual side modes, forever. No upkeep.
- **Under Elemental Sight** the client draws ore buds around the Hollow that grow with ripeness and the working gnome; without the sight the Hollow is a plain block. The server changes no other block.
- **Data cable**: a working Hollow is a producer of its yield with no inputs.
- **Journal**: binding a gnome reports a `bind` event; Schwaz gets its last task, binding a first gnome; the book tells of the gnomes.

## Capabilities

### New Capabilities
- `manufacturing/gnome-hollow`: the Hollow, veins, seeding, ripening, binding, working and the revealed vein.

### Modified Capabilities
- `manufacturing/journal`: the `bind` event and Schwaz's third task.

## Non-goals

- Gnome upkeep or offerings over time (PLAN.md section 5, open).
- Late veins (gold, diamond) and their stages.
- Final art for buds and the gnome.

## Impact

- Code: `alchemy/hollow` (block, block entity, menu, vein data), client renderer registered with the reveal hook and a client tracker of Hollows.
- Data: vein JSON, the Athanor craft for the Hollow, lang, models, loot, tags.
- Docs: Manufacturing page and a vein datapack page.
