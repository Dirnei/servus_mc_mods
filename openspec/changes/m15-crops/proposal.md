## Why

Beer needs barley and hops, and Servus: Brewery has neither yet (PLAN.md, Open: M15 Brewery, `m15-crops`: "barley, and hops on a placed pole"). The Malting Floor, the Brewhouse and the beers of the later M15 changes all start from these two crops, so they come first.

## What Changes

- **Barley**: a crop like vanilla wheat with eight growth stages on farmland, planted from Barley Seeds, harvested as Barley plus seeds when ripe and as seeds only when young (loot by age), bone meal grows it. It is a plain vanilla age crop, so the Shuttles Field Head and Harvester work it like wheat without any Shuttles change.
- **Hop Pole** (Hopfenstange): a thin wooden pole block crafted from sticks that stacks at most three blocks tall.
- **Hops on the pole**: a Hop Rhizome used on a pole that stands on farmland or dirt plants a vine at its foot. On random ticks the vine leafs out and climbs the pole one block at a time; once it has reached the top, every upper part of the pole grows green Hop cones. Right click on a part with cones picks Hops (from a harvest loot table) and keeps the vine. Breaking the foot drops the Hop Rhizome. Bone meal makes the next growth step. Leaves wrap the pole on planes offset from its faces.
- **First seeds**: breaking short grass sometimes drops Barley Seeds and, rarely, a Hop Rhizome; plains and taiga village house chests can hold both. Both through Global Loot Modifiers that add Brewery loot tables, so the chances live in data.
- Items Barley, Barley Seeds, Hops, Hop Rhizome and Hop Pole with textures drawn by `tools/brewery-textures/gen.py` in the mockup style (Barley and Hops port the approved `gerste` and `hopfen` sprites), `en_us` names, `c:` tags (`c:crops`, `c:crops/barley`, `c:crops/hops`, `c:seeds`, `c:seeds/barley`), vanilla tags for crops, farmers and chickens, and compostable values through the NeoForge data map.
- Pure growth rules of the vine (`HopVine`) with JUnit tests; GameTests for barley growth and drops and for hops climbing and picking.

## Capabilities

### New Capabilities
- `brewery/crops`: barley as a field crop, the Hop Pole, the hop vine that climbs it and bears cones, and how players first find seeds and rhizomes.

### Modified Capabilities
None. The Brewery creative tab requirement of `brewery/mod-setup` (still a delta of the unarchived `m15-brewery-project`) already says the tab lists every Brewery item, so the new items need no spec change there.

## Non-goals

- Malt, the Malting Floor and grist (`m15-malting`); the Brewhouse (`m15-brewhouse`); beers and yeast (`m15-beer`).
- Villager trades for seeds or hops.
- Hops as a Shuttles field crop: the vine needs a pole, so the Field Head and the Harvester leave hop poles alone. No Shuttles code changes.
- Wild hop plants in worldgen.
- Any use of hops or barley in recipes beyond composting.

## Impact

- New in `brewery/`: `crop/` (Barley crop block, Hop Pole block, pure `HopStage`, `HopStep`, `HopVine`), `registry/ServusBreweryBlocks`, more items in `registry/ServusBreweryItems`, datagen providers for block states and models, loot tables, loot modifiers, recipes, block and item tags and the compostables data map, GameTests, JUnit tests, textures.
- Changed: `tools/brewery-textures/gen.py` (new sprites), the Brewery mod class and datagen entry point.
- No Foundation, Alchemy, Shuttles or Delicacies source changes.
