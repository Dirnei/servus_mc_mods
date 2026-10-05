# Design

## Context

All Tier 1 content exists. The gameTestServer run config exists since M0 but no test. See proposal.md.

APIs verified in the NeoForge 21.1.255 sources: `GameTestHelper.setBlock`, `getBlockEntity`, `runAtTickTime`, `succeed`, `fail`; `@GameTest(template, timeoutTicks)`; `@GameTestHolder(modid)` and `@PrefixGameTestTemplate(false)`; `RegisterGameTestsEvent.register(Class)`; `StructureTemplateManager.get(ResourceLocation)` loads `data/<ns>/structure/<name>.nbt`.

## Decisions

### D1: Empty template, blocks placed in code
A 12 by 6 by 12 all air structure `servus_manufacturing:empty_12x6x12`, generated as NBT by a script. The test builds the layout with `setBlock`, which keeps the layout readable in code and avoids editing structures in game.

### D2: Layout
Relative coordinates, deposits at y 1, machines at y 2. Iron pairs at z 1 and z 3, copper pair at z 5, coal extractor at z 7, all extractors at x 1 on their deposits with smelters at x 2 (adjacent push). A duct column at x 3 from z 1 to z 7 carries ingots to assemblers at x 4 (z 1 gear, z 3 wire, z 5 pack) and packs to the lab at x 4 z 7. Generators at x 5 next to each assembler and the lab. The coal extractor feeds the duct column, which tops up smelter fuel. Every burner machine and generator starts with 64 coal.

### D3: Assertion window
Pipeline latency is about 16 seconds (ore 4 s, smelt 4 s, gear 1 s, pack 5 s, duct pushes), then one pack per 5 s. After 1200 ticks the lab should hold about 9 packs; the test accepts 7 to 13 so timing jitter passes while a broken ratio fails. Timeout 1300 ticks.

### D4: Registration
`ServusManufacturingGameTests` with `@GameTestHolder(ServusManufacturingIds.MOD_ID)` and `@PrefixGameTestTemplate(false)`, registered in the mod bus `RegisterGameTestsEvent` handler. The run config already enables the namespace.

## Pure Java vs Minecraft

All Minecraft; verified by running the GameTest server.

## Risks / Trade-offs

- [60 second test] -> Acceptable; it runs only in the GameTest server, not in `gradlew build`.
- [Rate depends on duct push cadence] -> The window is wide enough; if it flakes, widen or seed the pipeline.
