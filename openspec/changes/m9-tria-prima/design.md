# Design: m9-tria-prima

## Context

See proposal.md for why. After `m9-foundation-utilities` the shared machine base (`MachineBlockEntity`, `PoweredMachineBlockEntity`, `MachineStats`, `MachineDefaults`, `MachineMenu`, `MachineScreen`) lives in Foundation and Manufacturing registers its own machine defaults through `ManufacturingMachines`. The Crusher is the closest existing template: a powered machine with one input slot, a recipe type with a `SizedIngredient` and a time, a production capability and a JEI category. The pumps show how a machine exposes a fluid tank through `Capabilities.FluidHandler.BLOCK` and how `MachineScreen` draws a `FluidGauge`.

## Goals / Non-Goals

**Goals:**
- Principle fluids, Calciner and Coagulator working with pipes, tanks, buckets, the Sink, JEI and the data cable.
- All Factorio content of Manufacturing removed, the game and every test green after the change.

**Non-Goals:**
- Journal, Athanor, sight and gnomes (later M9 changes). Research Lab code stays untouched.
- New textures beyond simple placeholders; the fluids reuse the vanilla water textures with a tint.

## Decisions

### D1: Principles are NeoForge fluids with a tinted water look

Each principle registers a `FluidType` (in `NeoForgeRegistries.Keys.FLUID_TYPES`), a source and a flowing `BaseFlowingFluid` built from one `BaseFlowingFluid.Properties(type, source, flowing)` with `.block(...)` and `.bucket(...)`, a `LiquidBlock` and a `BucketItem`. `FluidType.Properties.create().canConvertToSource(false)` keeps them from making infinite sources. On the client `RegisterClientExtensionsEvent.registerFluidType(IClientFluidTypeExtensions, FluidType...)` returns `minecraft:block/water_still` and `water_flow` with a tint per principle (Salt white, Sulfur yellow, Mercury silver). Verified in the NeoForge 21.1.255 sources (`BaseFlowingFluid.Properties`, `FluidType.Properties`, `RegisterClientExtensionsEvent`, `IClientFluidTypeExtensions.getTintColor`).

Alternative: principles as items (dusts). Rejected by PLAN.md 2.1, which makes them fluids so the Fluid Pipe, Tank and Sink do the logistics.

### D2: A small pure model of the three principles

`Principles` (pure Java, no Minecraft): a value type of three non-negative mB amounts with `plus`, `fitsInto(capacity, current)`, `covers(needed)` and `minus`. The Calciner and Coagulator tanks and the recipe records use it, and it is covered by JUnit tests. `Principle` is an enum Salt, Sulfur, Mercury.

### D3: One machine fluid handler with three tanks

`PrincipleTanks` holds three `FluidTank`s, each restricted to its principle's fluid (`FluidTank` with a validator). It implements `IFluidHandler` with three tanks; a mode flag says whether it accepts (Coagulator) or gives (Calciner) through the capability. The block entity returns it on sides whose side mode allows insertion (Coagulator) or extraction (Calciner). The Calciner pushes into touching fluid handlers on Auto and Push sides each tick it holds fluid, one principle after another, the same way the pumps push water.

### D4: Calcination and coagulation recipe types

- `CalcinationRecipe(SizedIngredient input, Principles output, int time)`, type `servus_manufacturing:calcination`, `SingleRecipeInput`.
- `CoagulationRecipe(Principles input, Optional<SizedIngredient> ingredient, ItemStack result, int time)`, type `servus_manufacturing:coagulation`.

Both have a `MapCodec`/`StreamCodec` serializer like `CrushingRecipeSerializer`, and datagen builders. Principles in JSON are `{"salt": 100, "sulfur": 0, "mercury": 10}` with missing keys as 0.

### D5: The Coagulator's recipe selection

The selected recipe id is stored in the block entity and in the dismantled item (like the side configuration). The GUI lists every coagulation recipe sorted by result name, scrolls with the mouse wheel, and selects with `menu.clickMenuButton` (button ids above the side panel's ids, index into the sorted list the server builds the same way), so no new payload is needed.

Alternative: the item in the ingredient slot picks the recipe. Rejected: most Nigredo recipes have no item ingredient.

### D6: Production capability without fluids

Both machines implement `ProductionProvider`. The Calciner reports one recipe per calcination recipe with the first item of its ingredient in and nothing out, so the CPU can feed it but never waits for an output. The Coagulator reports only its selected recipe with its item ingredient (if any) and its result; principles are expected by pipe. The data cable only moves items, so fluids cannot be ordered.

### D7: Removal

Delete `deposit`, `machine/{extractor,smelter,crusher,assembler}`, `recipe/*` (assembling and crushing), `AssemblerRecipeInput`, `ConfigureRecipePayload`, `PatternSlot`, `AssemblerScreen`, their menus, JEI categories and transfer handler, the assembling and crushing datagen builders and recipes, deposit, biome modifier and worldgen JSON, tech JSON, the four item textures and the Factorio GameTests (reference block, energy channel test moves to Foundation with an Electric Pump as consumer). The Research Lab, tech system and tech tree screen stay for `m9-journal`.

### D8: Docs Balance lab keeps working

The lab and its tests read `docs/src/data/gamedata.json`. Its tests switch to a copy of today's file at `docs/src/lib/fixtures/tier1-gamedata.json`, so the lab logic stays tested until M9 item 7 rebuilds it on alchemy data. The live export keeps its schema; deposits and techs become empty lists. Calcination and coagulation recipes are not part of the export yet (item 7).

### D9: Machine stats

`calciner` and `coagulator` register through `ManufacturingMachines` as powered machines: work ticks unused (recipe time rules), 20 FE/t, 8,000 FE buffer, `fluid_capacity` 4,000 mB per tank. Stats files under `data/servus_manufacturing/machine/`.

### Pure and Minecraft logic

Pure, JUnit: `Principles` (sum, fit, cover, subtract, JSON style defaults through a plain parser helper). Minecraft, GameTest: Calciner turns cobblestone into principles, waits with full tanks, pushes into a Fluid Tank; Coagulator refuses foreign fluid, makes a Salt Crystal from piped Salt, waits without Mercury, keeps its selection; buckets fill from a tank; both machines report production recipes. Manual: GUIs, JEI, fluid look.

## PLAN.md decisions settled

Settles from PLAN.md 2.1: how the Tria Prima is played in Nigredo (Calciner and Coagulator as described), and that metals do not come from principles in Nigredo. PLAN.md section 5 open decisions are not affected. PLAN.md already lists this change; no update needed.

## Assumptions to confirm with the user

1. **Principle ratios** of the shipped calcination recipes (examples per item): cobblestone and stone 100 Salt, 10 Mercury; dirt 50 Salt, 20 Sulfur, 20 Mercury; sand and gravel 80 Salt, 10 Mercury; logs 20 Salt, 100 Sulfur, 10 Mercury; planks 5 Salt, 25 Sulfur; leaves and saplings 40 Sulfur, 20 Mercury; crops and seeds 10 Salt, 30 Sulfur, 30 Mercury; coal and charcoal 200 Sulfur; raw iron, copper and gold 150 Salt, 50 Mercury; redstone 100 Mercury; bone 100 Salt. Time 80 ticks each.
2. **Coagulator products in Nigredo**: Salt Crystal (250 Salt), Sulfur (250 Sulfur), Quicksilver (250 Mercury), clay ball (100 Salt, 50 Mercury), flint (150 Salt), gunpowder (150 Sulfur, 50 Salt), redstone (50 Salt, 150 Mercury), glowstone dust (100 Sulfur, 100 Mercury), bone meal (60 Salt, 20 Sulfur), slime ball (50 Sulfur, 150 Mercury). 40 ticks each. No metal from principles.
3. **No fallback calcination**: items without a recipe are refused instead of giving a little Salt.
4. **Both machines are FE powered** at 20 FE/t (PLAN.md keeps FE; Salamanders may change fire later).
5. **Recipes**: Calciner from bricks, iron, a furnace and a cauldron; Coagulator from iron, glass, a cauldron and redstone. No research.
6. **Principle look**: tinted vanilla water textures; principle buckets can be poured into the world and flow but never form new sources.
7. **Coagulator recipe pick** by clicking a list in its GUI; the CPU sees only the selected recipe.
8. **Research Lab stays** in the game without any tech until `m9-journal`.
9. **Names**: the fluids are Salt, Sulfur and Mercury; the items are Salt Crystal, Sulfur and Quicksilver.

## Risks / Trade-offs

- [Large removal breaks many tests at once] → Remove in steps that each build: content first, then data, GameTests and docs tests in the same task as the code they cover.
- [The export or docs build chokes on fluid recipes] → The export skips the two new types (D8); docs tests run on the fixture.
- [Recipe list GUI work grows] → A plain scrolled list of result icons with tooltips, no search.
- [Unbalanced numbers] → All numbers are datapack JSON and listed as assumptions; the balance pass is M9 item 7.

## Migration Plan

None. Dev worlds lose deposits and the removed machines.
