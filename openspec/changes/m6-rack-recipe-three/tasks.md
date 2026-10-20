# Tasks

Paths are relative to `shuttles/src/main/java/io/github/dirnei/mc/servus/shuttles/`. GameTests run with `gradlew :shuttles:runGameTestServer`.

## 1. Recipe

- [x] 1.1 `datagen/ServusShuttlesRecipeProvider.java`: rack result count 3 (design.md D1). Run `gradlew :shuttles:runData`, check `data/servus_shuttles/recipe/heavy_rack.json` has count 3, run `gradlew build`.
- [x] 1.2 `gametest/HeavyRackGameTests.java`: `rackRecipePicksWood` asserts a count of 3 and the wood on the stack; adjust `gametest/PatternResultGameTests.java` and any other GameTest that counts racks from the rack recipe or pattern (design.md D2). Run the Shuttles GameTests.
- [x] 1.3 Docs game data: regenerate `docs/src/data/gamedata.json` the way `runData` exports it and run the docs tests if a recipe count test exists. Run `gradlew build`.

## 2. Verify

- [x] 2.1 Full `gradlew build`, `gradlew test` and the Shuttles GameTests green; the game launches with all three mods.
- [ ] 2.2 Play check by the user: crafting a rack gives three, each with the plank wood.
- [x] 2.3 Play check fix: the rack item showed its back board face on and read as a plank; its inventory model gets the `minecraft:block/block` parent so the GUI shows the turned rack (`datagen/HeavyRackModels.java`). The Rail Elevator item had no display settings either; its frame model gets the same parent (`datagen/ElevatorModels.java`). Run `gradlew :shuttles:runData`, `gradlew build`.
- [x] 2.4 Play check fix: the Rail Elevator item showed only the four posts; it gets its own model with the posts, the head and a straight rail on its floor (sleepers ending at the posts), and the head ring now runs between the post tops instead of over them, so the top of a shaft no longer has the ring and post tops in one plane (`datagen/ElevatorModels.java`). Run `gradlew :shuttles:runData`, `gradlew build`.
