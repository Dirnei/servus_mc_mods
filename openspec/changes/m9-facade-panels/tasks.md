# Tasks

## 1. Pure logic

- [ ] 1.1 `DuctShapes.panel(side, hollow)` in `foundation/.../logistics/duct/DuctShapes.java` and the pure `PanelHit` in `foundation/.../logistics/duct/PanelHit.java` (design D3, D4). JUnit in `foundation/src/test/.../logistics/duct/`: `DuctShapesPanelTest` (solid slab is one pixel on each of the six faces, hollow frame is four boxes leaving pixels 4 to 12 open, all boxes inside 0 to 16) and `PanelHitTest` (hit in a slab picks that face, the clicked face wins on an edge shared by two slabs, a hit in the middle or on a face without a panel gives none). Verify: `gradlew test`.

## 2. Panels on the duct

- [ ] 2.1 `UtilityDuctBlockEntity`: replace `facade` with six panel slots and the hollow mask, `Panels` NBT, `closed(side)`, model data `PANELS` with a `PanelSet` record in `DuctModelData` (design D1). Update every `facade()` caller to compile: `UtilityDuctBlock`, `UtilityDuctModel`, `DuctBlockColor`, `DuctOverlayRenderer`. Verify: `gradlew build`.
- [ ] 2.2 Closing (design D2): `computeArm`, `itemHandler`, `fluidHandler` and `dataSideOpen` respect `closed`; the item, energy and fluid network types in `logistics/network/` get the closing `ConnectionRule`; a panel change refreshes the graph edges of the duct and of the duct behind that face. Verify: `gradlew build`.
- [ ] 2.3 Items and recipes (design D3, D7): `FacadeItem` with a hollow flag and per face placement, `HOLLOW_FACADE` in `ServusLogisticsItems`, `HollowFacadeRecipe` in `logistics/facade/` registered in `ServusLogisticsRecipes`, datagen in `LogisticsDataParts` (special recipe, hollow item model, lang "Hollow Facade" and "Hollow Facade (%s)"), a `hollow_facade.png` item texture in `assets/servus_foundation/textures/item/`. Verify: `gradlew runData`, then `gradlew build`.
- [ ] 2.4 `UtilityDuctBlock` (design D3, D4, D5): panel boxes in the shape with a `long` cache key, sneak carrot removes the panel picked by `PanelHit` or else the duct, `onRemove` drops every panel, `getAppearance` per face. Verify: `gradlew build`.
- [ ] 2.5 GameTests in `foundation/.../logistics/gametest/ServusLogisticsGameTests.java`, replacing the facade half of `facadeRecipeAndApplication`: `panelOnClickedFace` (second panel on that face refused, other faces free), `solidPanelSplitsDucts` (two item ducts, two networks and no arms, removal joins them), `solidPanelClosesEndpoint` (no endpoint and no item handler toward a chest), `hollowPanelKeepsNetwork`, `carrotRemovesOnePanel`, `breakingDropsPanels`, `hollowRingRecipe` (eight stone facades give eight hollow; a mixed ring gives nothing). Verify: `gradlew runGameTestServer` green.

## 3. Rendering

- [ ] 3.1 `UtilityDuctModel` and `DuctBlockColor` in `foundation/.../logistics/client/` (design D5): baked panel quads per face from the panel block's quads with `FaceBakery`, cached per state, face and hollow; render type union and per type quads; panel tint indices from 200 decoded by `DuctBlockColor`. `DuctOverlayRenderer` picks ducts with panels (design D6). Verify: `gradlew build`, then the client from main: stone and grass block panels, a hollow panel with an arm through it, six stone panels look like stone.

## 4. Examples, JEI, plan and docs

- [ ] 4.1 `FacadeItem.hollowExamples()`, the creative tab in `ServusLogisticsEvents`, and in `ServusFoundationJeiPlugin` the hollow subtype interpreter, one ring example recipe per block and the hollow stacks on the information page; the info text in `LogisticsDataParts` gains the solid and hollow sentence (design D8). Verify: `gradlew runData`, then `gradlew build`.
- [ ] 4.2 `PLAN.md`: M9 item 1d `m9-facade-panels` and the facade line in section 2 describes per face panels, solid closing and hollow passing. `docs/src/content/docs/mods/foundation.md`: the Facade entry describes panels and the Hollow Facade. Verify: `openspec validate m9-facade-panels` and `npm run build` in `docs/`.
- [ ] 4.3 Play check by the user from main: panels on clicked faces, solid panel splits two ducts and cuts a chest, hollow panel passes, carrot removes one panel, outlines, overlay with goggles, JEI shows both kinds with their recipes. Verify: the user confirms in game.

## 5. Integration

- [ ] 5.1 Full gradle build, tests green, game launches with all three mods.
