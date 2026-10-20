# Tasks

## 1. Pure vine rules

- [x] 1.1 `crop/HopStage`, `crop/HopStep`, `crop/HopVine` (growth at a part, first growth, height rule) in `brewery/src/main/java/io/github/dirnei/mc/servus/brewery/`; JUnit `HopVineTest` in `brewery/src/test/java/...`: young leafs out, grown climbs onto a bare pole above, no cones before the top is leafed, cones on upper parts once the top is leafed, foot never bears cones, single pole never bears cones, bare never grows, climb before cones, first growth from the foot up, empty when fully grown, fourth pole refused; verify `gradlew :brewery:test`

## 2. Textures

- [x] 2.1 `tools/brewery-textures/gen.py`: port `ellipse`, `poly`, `paint`, the `gerste` and `hopfen` sprites, add `barley_seeds`, `hop_rhizome`, `hop_pole` items and the block sprites `barley_stage0..7`, `hop_pole`, `hop_vine_young`, `hop_vine`, `hop_cones` (alpha 0 or 255 only); write the PNGs, check them in an 8x preview; verify `gradlew build`

## 3. Blocks and items

- [x] 3.1 `crop/BarleyBlock`, `crop/HopPoleBlock` (properties, shapes, support, height rule, rhizome planting, picking, random ticks, bone meal), `registry/ServusBreweryBlocks`, items in `registry/ServusBreweryItems` (Barley Seeds, Barley, Hop Rhizome, Hops, Hop Pole, tab order), registered from `ServusBrewery`; verify `gradlew build`

## 4. Datagen

- [x] 4.1 `datagen/ServusBreweryBlockStateProvider` (barley stages, hop pole models per stage, item models), item models and lang for the new items and blocks; verify the face pair list of design.md against the generated models
- [x] 4.2 Loot: `datagen/ServusBreweryBlockLoot` (barley by age, hop pole with rhizome and hops), `ServusBreweryExtraLoot` (grass, village chests, hop harvest), `ServusBreweryLootModifierProvider` (short grass, plains and taiga house chests)
- [x] 4.3 `ServusBreweryBlockTagsProvider`, `ServusBreweryItemTagsProvider` (`c:` tags, villager and chicken tags), `ServusBreweryDataMapProvider` (compostables), `ServusBreweryRecipeProvider` (Hop Pole); run `gradlew :brewery:runData`, commit `src/generated/resources`; verify `gradlew build`

## 5. GameTests

- [x] 5.1 `gametest/BreweryCropGameTests` (barley grows with bone meal and drops by age; fourth pole refused; rhizome plants at the foot; bone meal climbs a three pole column and grows cones on the upper parts; right click picks Hops and keeps the vine; breaking the foot drops the rhizome), listed in `BreweryGameTests`; verify `gradlew :brewery:runGameTestServer` and `gradlew build test`

## 6. Play check

- [ ] 6.1 Play check by the user: barley on farmland grows, ripens golden and drops barley and seeds; Barley Seeds from short grass; Hop Poles stack three high; a rhizome on a pole on farmland grows a vine that climbs, leaves wrap the pole without flicker from several angles, cones appear on the upper parts, right click picks Hops; a Shuttles Field Head lists Barley Seeds and works a barley field, and does not list the Hop Rhizome
