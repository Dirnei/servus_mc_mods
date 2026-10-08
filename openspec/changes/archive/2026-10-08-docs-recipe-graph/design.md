# Design

## Context

- `GameDataExportProvider` (Manufacturing datagen) runs last in `:manufacturing:runData`. It reads only Manufacturing's generated `data/servus_manufacturing/recipe` folder and drops every recipe type other than assembling, crushing, smelting and blasting ("crafting recipes are build costs"). Item names come from `BuiltInRegistries.ITEM` plus the vanilla and Manufacturing language files.
- Manufacturing's data run loads Manufacturing, Lib and Logistics (`manufacturing/build.gradle`, `loadedMods`). Storage is not loaded, so its items are not in the registry; `BuiltInRegistries.ITEM.get` returns air for them.
- Every mod commits its generated resources under `<mod>/src/generated/resources`, so the other mods' recipe and language JSON can be read as plain files.
- The lab already has a dagre-based `Graph` component (`docs/src/components/lab/Graph.tsx`, layered, `LR` or `TB`, foreignObject node bodies, edge labels, `.lab-graph` scrolls horizontally) and reads the committed data through `docs/src/lib/committed.ts`.
- Recipe JSON shapes in this repo: shaped (`pattern`, `key`, `result`; also `servus_storage:heavy_rack`), shapeless (`ingredients`, `result`), and special (`servus_logistics:facade`, only `type` and `category`).

PLAN.md leaves no open decision for this change. It is docs tooling under the "Documentation site and Balance lab" entry in section 4.

## Goals / Non-Goals

**Goals:**
- One export run that sees the recipes and names of all four mods without adding a Gradle or mod dependency.
- Keep the chain view and ratio math unchanged.
- Keep the graph logic pure and covered by vitest; keep the recipe JSON reading pure and covered by JUnit.

**Non-Goals:**
- Icons for tags.
- Showing lab edits or drafts in the graph (see proposal, `docs-recipe-editor`).

## Decisions

### D1: Read the other mods' generated folders as files, named by a system property
The Manufacturing data run gets a new system property `servus.gamedata.generated`: the four `src/generated/resources` folders of lib, logistics, manufacturing and storage, joined with `File.pathSeparator` in that fixed order. For Manufacturing it points at the folder this run is writing, so its fresh output is used as today.
- *Alternative:* load Storage in the Manufacturing data run. Rejected: it adds a run-time dependency that the dependency rules forbid, just for docs.
- *Alternative:* each mod exports its own gamedata fragment and the site merges them. Rejected: four providers and a merge step for what is one read over committed JSON.

### D2: Data run order
`:manufacturing:runData` gets `mustRunAfter` on `:lib:runData`, `:logistics:runData` and `:storage:runData`, so `gradlew runData` writes every mod's recipes before the export reads them. A single `:manufacturing:runData` (as in CI) still reads the committed files of the others.

### D3: Crafting recipes go in a separate `crafting` section; schema 2
New top-level section `crafting`, keyed by recipe id, sorted:
```
"servus_logistics:power_cable": {
  "type": "minecraft:crafting_shaped",
  "pattern": ["CCC", "RGR", "CCC"],
  "key": { "C": {"item": "minecraft:copper_ingot"}, "G": {...}, "R": {...} },
  "inputs": [ {"item":"minecraft:copper_ingot","count":6}, ... ],
  "outputs": [ {"item":"servus_logistics:power_cable","count":8} ]
}
"servus_logistics:facade": { "type": "servus_logistics:facade", "special": true, "inputs": [], "outputs": [] }
```
`pattern` and `key` only for grid recipes, `special` only when true. Inputs are summed per item or tag and sorted by key.
- *Alternative:* put crafting recipes into `recipes`. Rejected: the chain view would see them as routes with no machine (the Assembler's `minecraft:crafting` type does not match `minecraft:crafting_shaped`), and their inputs would pull extra vanilla smelting recipes into the closure.
- `GameDataJson.SCHEMA` and the site's `GAMEDATA_SCHEMA` go to 2 together. Proposal files keep `PROPOSAL_SCHEMA`; their `data.schema` field just records 2 from now on.

### D4: Classify recipe files by shape, not by type
For each `data/<ns>/recipe/**/*.json` under the four folders, the recipe id is `<ns>:<relative path>`. Assembling, crushing, smelting and blasting go to `recipes` as today, now from all four folders. Everything else goes to `crafting`: `pattern` plus `key` means grid, `ingredients` plus `result` means shapeless, anything else means special. That covers the heavy rack and future Servus wrappers without listing types. A list ingredient keeps its first entry, as the current `ingredient` helper does. This lives in a new pure class `CraftingRecipeReader` (Gson only, no Minecraft imports) with JUnit tests.

### D5: Names by language key, not by registry
Language maps load in this order: vanilla `en_us.json` from the classpath, then each of the four `assets/<ns>/lang/en_us.json` from the generated folders. A `servus_*` id looks up `item.<ns>.<path>`, then `block.<ns>.<path>`, then falls back to title case. Vanilla ids keep the registry description id lookup. This fixes Storage names without loading Storage.

### D6: Graph model is pure TypeScript
`docs/src/lib/recipegraph.ts`, no DOM or Preact:
- `modOf(id)`: `lib`, `logistics`, `manufacturing`, `storage` from the `servus_*` namespace, else `vanilla` (tags included).
- `buildRecipeGraph(data, visibleMods)`: recipe nodes for every `recipes` and `crafting` entry whose namespace starts with `servus_` and whose mod is visible; item nodes for every stack key they touch; edges input to recipe and recipe to output with counts.
- `ancestors(graph, itemKey)`: node and edge ids reached backwards.
- `rawCost(graph, itemKey, routes)`: map from vanilla item or tag key to amount per one item, plus `noRecipe` and `cycles` sets and the list of items with more than one producing recipe. Expands only Servus items, divides by the recipe's output count of that item, tracks the current path to stop cycles. The default route is the first producing recipe by id; `routes` overrides per item.
- Recipe kind label from type: `servus_manufacturing:assembling` Assembler, `servus_manufacturing:crushing` Crusher, `minecraft:smelting` Smelting, `minecraft:blasting` Blasting, any crafting entry Crafting, special entries their item name plus "special", otherwise the type id.

### D7: Rendering reuses `Graph`
`RecipeGraph.tsx` draws with `Graph` in `LR` direction. `GraphNode` gets an optional `className` for the mod colour and `GraphEdge` an optional `dimmed` flag. Highlighting uses the existing `muted` tone for dimmed nodes. Item nodes are wider than the small recipe nodes. Edge labels only for counts above one. Mod colours are CSS tokens in `lab.css` with light and dark values. The filter is four checkboxes. The item panel sits next to the graph on wide screens and below it on narrow ones; shaped recipes are drawn as a CSS 3x3 grid of short item names. Tags are shown as `#` plus their path, for example `#planks`. Escape clears the selection.

The page reads `gameData` from `committed.ts` directly, not the lab draft store.

### D8: Pan and zoom with our own transform
`Graph` gets an opt-in `zoomable` mode, so the chain and research pages keep their scroll box. In that mode the SVG fills a fixed-height box and an inner `<g>` carries `translate(x, y) scale(k)` from Preact state:
- the wheel zooms around the cursor; dragging pans with pointer capture; two pointers pinch
- `k` is limited to 0.1 to 3
- Fit computes the transform from the dagre bounds; a `focus` prop (node ids) fits their union
- a drag of more than a few pixels does not count as a click, so node clicks still select

The graph starts fitted, and selecting an item focuses its ancestors.
- *Alternative:* `@panzoom/panzoom`. Rejected: it transforms the DOM element with CSS, which fights the foreignObject buttons and their click handling, and it adds a dependency for about 80 lines of code.

### D9: Icons rendered in game by a dev run
`IconExporter` lives in Manufacturing's client code (next to the game data export) and is registered on the client only when `servus.icons.output` is set. On the first `ScreenEvent.Render.Post` of the title screen it reads the item ids from `servus.icons.gamedata` (`gamedata.json`), then for each item:
1. bind a `TextureTarget(64, 64, true, Minecraft.ON_OSX)`, clear it to transparent
2. set an orthographic projection from 0 to 16 with `VertexSorting.ORTHOGRAPHIC_Z` and the GUI model view translation, as `GameRenderer` does for the GUI, with `Lighting.setupFor3DItems()`
3. draw with `new GuiGraphics(mc, mc.renderBuffers().bufferSource()).renderItem(stack, 0, 0)` and `flush()`
4. read the pixels like `Screenshot.takeScreenshot`: `NativeImage.downloadTexture(0, false)`, `flipY()`, `writeToFile(path)`

Then it restores the main render target and calls `Minecraft.stop()`. The root `build.gradle` gets a client run `iconExport` with all Servus mods, its own game directory and both properties. `docs/.gitignore` ignores `src/icons/minecraft/`.

The site loads icons with `import.meta.glob('../../icons/**/*.png', { eager: true, query: '?url', import: 'default' })`, so a missing vanilla folder is just an empty set and nothing 404s. Icons are drawn with `image-rendering: pixelated`.
- *Alternative:* flat textures from `textures/item`. Rejected: 25 of 38 Servus items are 3D block models without an item texture.
- *Alternative:* committing vanilla icons. Rejected by the user: Mojang assets stay out of the repository and the published site.

### APIs used
No new NeoForge API. The export keeps using `DataProvider`, `PackOutput.getOutputFolder(PackOutput.Target.DATA_PACK / RESOURCE_PACK)` and `BuiltInRegistries.ITEM` for vanilla names, all already in use and building against NeoForge 21.1.x in this repo.

Icon export (all verified in the NeoForge 21.1.255 sources): `ScreenEvent.Render.Post`, `com.mojang.blaze3d.pipeline.TextureTarget`, `GuiGraphics(Minecraft, BufferSource)`, `GuiGraphics.renderItem(ItemStack, int, int)`, `GuiGraphics.flush()`, `RenderSystem.setProjectionMatrix(Matrix4f, VertexSorting)`, `NativeImage.downloadTexture`, `flipY`, `writeToFile(Path)`.

### Testing split
- Pure Java, JUnit: `CraftingRecipeReader` (shaped, shapeless, special, Servus grid type, tag and list ingredients, summing), `GameDataJson` (crafting section output, schema 2).
- Touches Minecraft, checked by running the data run: `GameDataExportProvider` wiring, folder list, names. The result is checked in the regenerated `gamedata.json`.
- Pure TypeScript, vitest: `recipegraph.ts`.
- Visual check in `npm run dev`: the page, colours, filter, highlight, panel, phone width, zoom and pan, icons.
- Icon export, touches Minecraft and the GPU: checked by running `gradlew iconExport` and looking at the PNGs.

## Risks / Trade-offs

- [Another mod's generated folder is stale, so the export reads old recipes] → D2 orders `gradlew runData`. CI's stale check still only compares `gamedata.json`, as today.
- [About 70 recipes and 60 items give a tall graph] → the mod filter and the ancestor highlight keep it readable. Pan and zoom (D8) let the user move around it.
- [Dagre crossings make long edges hard to follow] → the highlight dims everything that does not matter for the selected item.
- [A future recipe type with a grid under other field names would be classified special] → it still shows up as a marked node, so it is visible rather than silently missing. The reader can learn the shape then.

- [Icons go stale when a texture or model changes] → rerun `gradlew iconExport` and commit the Servus icons. CI does not check icons.
- [An item renders empty, for example one drawn by a custom renderer that needs a level] → the PNG is transparent; the site then shows the name only. Checked by looking at the exported icons.

## Migration Plan

None. The site and `gamedata.json` change in the same commit. Nothing is released.
