## Why

The Delicacies kitchen has crops but nothing to cook them on (PLAN.md, Open: M14 Delicacies, `m14-tiled-stove`). Every later dish change (`m14-bakery`, `m14-meat-dishes`, `m14-sides`, `m14-teller`) and the Brewery's Brewhouse (`m15-brewhouse`) needs the Tiled Stove, its firebox and the cooking vessels first, and the Heating Element (`m14-heating-element`) needs a firebox to plug into.

## What Changes

- **Stove Tile** block, cheap on day one (Clay Balls around a stone), and the **Tiled Stove** (Kachelofen): a straight row of two or more Stove Tiles up to a maximum length from data (example 4), formed with the Foundation Configuration Carrot. The clicked tile gets the brass firebox door on the face the player looks at; every tile of the row gets an iron cooking plate with a hob ring on top. Breaking or dismantling any tile unforms the stove. A single tile or a row that is too long is refused with a message.
- **Firebox**: one per stove, with a fuel slot that takes vanilla furnace fuels by right click and through `Capabilities.Item` on every tile. It lights a fuel item only when a vessel on the stove has something to cook, burns it down like a furnace, and its door glows (light) and flickers while it burns. A small, Minecraft free firebox interface answers "is it hot" for every cooking spot, with a seam where the Heating Element will later swap fuel for FE and add a preheat curve. The Brewery reuses the same interface and fire logic.
- **Pot** (family steel, two handles) and **Pan** (iron, wooden handle): placed on a stove tile, they cook only while the firebox below is lit. Data driven recipe types `servus_delicacies:pot` (counted items, an optional fluid such as water, time) and `servus_delicacies:pan` (counted items, time). Ingredients go in by right click or `Capabilities.Item`; water goes into the pot from a bucket or through `Capabilities.Fluid`; results show in the vessel and come out by an empty hand right click or `Capabilities.Item`. Steam rises while a vessel cooks.
- **Cutting Board** and **Kitchen Knife**: items placed on the board are cut by a right click with the knife, one item per click, by data driven `servus_delicacies:cutting` recipes; the board also takes input and gives its pieces through `Capabilities.Item`.
- Food items with their hunger and saturation in a datapack data map (`servus_delicacies:food`), so balance stays in JSON: Boiled Potatoes, Fried Egg, Fried Onions, Fried Potatoes. Ingredient items: Chopped Onion and Shredded Cabbage.
- Starter recipes: Stove Tile, Pot, Pan, Cutting Board, Kitchen Knife (crafting); cabbage to Shredded Cabbage and onion to Chopped Onion (cutting); potatoes and water to Boiled Potatoes (pot); egg to Fried Egg, Chopped Onion to Fried Onions, Boiled Potatoes with Chopped Onion to Fried Potatoes (pan).
- Kitchen balance numbers in `data/servus_delicacies/kitchen/stats.json` (stove maximum length, pot tank size).
- JEI categories for pot, pan and cutting recipes (soft dependency, API at compile time only).
- Textures from `tools/delicacies-textures/gen.py`: tile, door (lit and flickering), cooking plate (cold and lit), food and knife sprites. Pot and Pan use the Foundation steel and heavy iron textures.

## Capabilities

### New Capabilities
- `delicacies/tiled-stove`: the Stove Tile, forming and unforming the Tiled Stove, its firebox, fuel, lit look and the "is it hot" contract cooking spots rely on.
- `delicacies/cooking`: the Pot and the Pan, the pot and pan recipe types, hand and automation use, and the cooked foods with data driven food values.
- `delicacies/cutting-board`: the Cutting Board, the Kitchen Knife and the cutting recipe type.

### Modified Capabilities
- None. The creative tab requirement of `delicacies/mod-setup` already covers every new item.

## Non-goals

- The Heating Element, FE input, the brass thermometer and the preheat curve (`m14-heating-element`); this change only leaves the seam.
- Flour, Pretzel, dumplings, meat dishes, Sauerkraut and plated meals (`m14-bakery`, `m14-meat-dishes`, `m14-sides`, `m14-teller`).
- The Brewhouse and its Brew Kettle and Malt Kiln (`m15-brewhouse`).
- Raw vegetables as food; the food data map only covers the cooked foods of this change.
- A GUI for any kitchen block.
- A chimney or flue on the stove.

## Impact

- New Delicacies packages: `firebox` (pure fire logic and the firebox interface), `stove` (Stove Tile, forming, block entity), `cooking` (vessels, recipes, slot matching), `board` (Cutting Board, knife, cutting recipe), `food` (food item and data map), `kitchen` (stats manager), client renderers, JEI plugin, GameTests, JUnit tests.
- Changed: `ServusDelicacies` (registers, capabilities, recipe sync, reload listener), registry classes, datagen providers (models, block states, loot, recipes, tags, data maps, lang), `delicacies/build.gradle` (JEI API compile only), `tools/delicacies-textures/gen.py`.
- No Foundation, Alchemy, Shuttles or Brewery source changes.
