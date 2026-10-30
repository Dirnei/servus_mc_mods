# Proposal

## Why

Delicacies has crops, a stove and dishes, but fish only come from fishing by hand. Players asked for an aquarium that breeds fish on its own: a bigger body of water where a stock grows, needs feeding and clean water, and can be harvested by hand or by any pipe network. The Foundation Fluid Tank already holds large rectangular bodies of water, so the aquarium builds on it instead of adding another container.

PLAN.md milestone M14 Delicacies has no aquarium item yet; this change adds `m14-aquarium` to the M14 list (explored 2026-10-29) and grooms it at archive.

## What Changes

- New blocks **Aquarium Floor** (full block, copper cabinet with a gravel top) and **Aquarium Lid** (4 px copper slab with a light panel underneath). A Fluid Tank holding water, with a full layer of Floor blocks under its whole footprint and a full layer of Lid slabs over its whole top, forms an **aquarium** when a player uses the Configuration Carrot on one of the parts. Breaking any part, the tank or changing the tank's box unforms it.
- **Fish stock**: one species per aquarium (cod or salmon). A fish bucket used on a part pours one fish in and gives an empty bucket back; a water bucket used on a part catches one fish into a fish bucket. The other species and a full aquarium are refused. A fish bucket on the tank glass pours the fish in too; other buckets on the glass keep filling and draining the tank as today.
- **Growth**: capacity follows the water in the tank (example: one fish per 4,000 mB). With two or more fish, fed and in clean water, the stock grows logistically toward the capacity once per cycle (example 1,200 ticks). Fish eat items from the new tag `c:fish_feed` (Delicacies fills it with `c:seeds`); unfed fish stop growing and later starve; too dirty water or too little water kills fish.
- **Water quality**: fish and eaten feed add dirt; quality is dirt against the water volume. A **Carbon Filter** (from `m14-carbon-filter`) in the aquarium cleans an amount per cycle that grows with the tank, and wears out; the spent filter moves to the catch slots. The water holding the cleaned dirt turns into the new Delicacies fluid **Murky Water**, kept in a hidden tank of the aquarium that parts drain; when it is full the filter stops. Without a filter the quality only falls; water drained through the glass takes no dirt, fresh water dilutes it.
- **Catch**: three catch slots. With auto catch on, fish above a keep level move into the catch slots as raw fish each cycle. Auto catch and the keep level are set in the GUI.
- **Power**: the aquarium needs FE for its heater, pumps and light, taken through any Floor block into one buffer. The cost grows with the tank and the lid (example 2 FE/t per tank block, 1 FE/t per lid block). Without power the light goes off and breeding and cleaning stop; fish still eat and dirt still grows.
- **One GUI** from any part: fullness, stock and capacity, water quality, power, the feed, filter and catch slots, a Murky Water gauge (JEI lookup on hover), auto catch and keep level.
- **Automation without other mods' code**: every part exposes the item slots, the fluid outlet and inlet, and FE input through the standard NeoForge capabilities, so pipes and Shuttles stations work with no Shuttles code.
- **Rendering**: the vanilla cod or salmon models swim inside the tank on smooth paths; the number drawn shows how full the aquarium is (example up to 16). Fish slow down in dirty water, float belly up when they die, flop when the water is gone; a gravel bed with swaying seagrass covers the tank floor; the lid panel glows while powered.
- Lid items placed on the top of a formed tank place a whole layer at once, like Fluid Tank items do.

## Non-goals

- New fish species (carp, trout, tropical fish) and warm water costs per species.
- A separate water filter block or any recycling of Murky Water (dump it in a Sink, or let another mod treat it).
- Light that changes breeding; the light is looks only.
- Fish dishes (Steckerlfisch and others) and fish feed recipes beyond the tag.
- Shuttles, Foundation or Fluid Tank code changes; Data Potato reports for the aquarium.
- Placing a whole layer of Floor blocks.

## Capabilities

### New Capabilities
- `delicacies/aquarium`: the Aquarium Floor and Lid, forming and unforming on a Fluid Tank, the fish stock and buckets, growth, feeding, water quality with filter and Murky Water, catch, power, GUI, capabilities for automation and the swimming fish look.

### Modified Capabilities
<!-- none: the Fluid Tank, Foundation filter API and other Delicacies specs keep their requirements -->

## Impact

- Depends on `m14-carbon-filter` (Foundation Carbon Filter item and filter API); apply after it.
- Shares Delicacies registries, datagen providers, lang and the client setup with the applied but unarchived Delicacies changes (`m14-delicacies-project`, `m14-crops`, `m14-tiled-stove`, `m14-heating-element`, `m14-bakery`, `m14-meat-dishes`, `m14-sides`, `m14-teller`); apply after those are archived to keep the diffs apart.
- New Delicacies code: package `aquarium` (blocks, block entities, pure growth logic, stats from JSON, menu and screen, payloads), `registry` additions (blocks, items, block entities, menu type, Murky Water fluid), `client` (aquarium renderer, screen, fluid client extension), `datagen` (models, blockstates, loot, recipes, tags `c:fish_feed`, lang), GameTests and JUnit tests.
- Reads the Foundation Fluid Tank through its public block entity and tank network API (box, content, fill, drain); no change to Foundation.
- New datapack file `data/servus_delicacies/aquarium/aquarium.json` with every balance number.
- Delicacies `CHANGELOG.md` gets an Added line at archive.
