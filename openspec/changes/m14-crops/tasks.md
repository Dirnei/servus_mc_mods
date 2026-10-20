# Tasks

## 1. Crop catalogue (pure logic)

- [x] 1.1 `crop/CropKind` (six crops, self planting flag, block, harvest, planting, seed ids, tag paths, texture names) and `crop/CropStages` (age to texture stage like vanilla carrots), no Minecraft imports; JUnit `CropKindTest` (ids unique, self planting crops plant their harvest, seeded crops plant `<name>_seeds`, Mustard's harvest is `mustard_seeds`, tag paths) and `CropStagesTest` (0,0,1,1,2,2,2,3 and out of range ages rejected); verify `gradlew :delicacies:test` and `gradlew build`

## 2. Textures

- [x] 2.1 `tools/delicacies-textures/gen.py`: port the mockup painter and `kraut_head`, `zwiebel`, `petersilie`, `kren`; add Mustard Seeds, three seed items and four stage textures per crop; write `textures/item/*.png` and `textures/block/<crop>_stage<0-3>.png`; verify the ported item sprites match the mockup pixel for pixel and look at an 8x preview sheet

## 3. Blocks and items

- [x] 3.1 `crop/DelicaciesCropBlock` (planting item supplier, codec), `registry/ServusDelicaciesBlocks` (six crop blocks from `CropKind`, vanilla carrot properties), crop items in `registry/ServusDelicaciesItems` (planting block items with the item description prefix, plain harvest items, tab order after the Pretzel), block register in `ServusDelicacies`; verify `gradlew build`

## 4. Datagen

- [x] 4.1 `datagen/DelicaciesBlockStateProvider` (crop models on `minecraft:block/crop`, variants per age, item models), language (`ServusDelicaciesLanguageProvider`: items and blocks); remove the item model provider if the block state provider covers all items; run `gradlew :delicacies:runData`; verify `gradlew build`
- [x] 4.2 `datagen/DelicaciesBlockLoot` (seeded crops with `createCropDrops`, self planting crops with the carrot shape), `datagen/DelicaciesExtraLoot` (`gameplay/grass_seeds`, `chests/village_seeds`), `datagen/DelicaciesLootModifierProvider` (`neoforge:add_table` on short and tall grass and the five village house chests); run `gradlew :delicacies:runData`; verify `gradlew build`
- [x] 4.3 `datagen/DelicaciesItemTagsProvider` (`c:crops/*`, `c:seeds/*`, `c:crops`, `c:seeds`, `minecraft:villager_plantable_seeds`, `minecraft:chicken_food`), `datagen/DelicaciesBlockTagsProvider` (`minecraft:crops`, `minecraft:maintains_farmland`), `datagen/DelicaciesDataMapProvider` (`neoforge:compostables`); run `gradlew :delicacies:runData`, commit `src/generated/resources`; verify `gradlew build`

## 5. GameTests

- [x] 5.1 `gametest/CropGameTests`: every crop plants on farmland from its planting item and not on dirt, bone meal ripens it and is refused when ripe, a ripe crop drops its harvest (and seeds for seeded crops), a young seeded crop drops exactly one seed, pick block gives the planting item, short grass broken by hand drops Delicacies planting items over many rolls and never with shears, the village seeds table gives planting items; register in `DelicaciesGameTests`; extend `DelicaciesSetupGameTests` to check the tab lists the crop items; verify `gradlew :delicacies:runGameTestServer` and `gradlew build test`

## 6. Play check

- [ ] 6.1 Play check by the user: break grass until Delicacies seeds drop and open a village house chest; plant all six crops on farmland, watch them grow and bone meal them; every stage texture looks right from several angles with no flicker; harvest ripe and unripe crops (with and without Fortune); compost them; set a Shuttles Field Head to Onion and Cabbage Seeds and let a Harvester harvest and replant
