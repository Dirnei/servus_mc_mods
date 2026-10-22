# Design: m9-anchor-ftb-chunks

## Context

`EnderAnchorBlockEntity.allowance()` returns `STATUS_FTB_CHUNKS` (3) and unregisters the anchor while `ServusFoundationConfig.enderAnchorRespectsFtbChunks()` is true and `ModList.get().isLoaded("ftbchunks")`. `SiteWakes` in Shuttles uses the same check to keep sites asleep. The option is `true` by default, so in the Alchemy GameTest run, which loads FTB Chunks as a dev runtime mod, the nine anchor GameTests (`anchorgametests.*`, `machinegametests.enderanchor*`) fail. The user decided to flip the default (option A). Respecting FTB Chunks claims (option B) becomes an open point in PLAN.md.

## Decisions

- **Default `false`.** Only the default in `ModConfigSpec.Builder.define` changes. The check itself moves into one place, `ServusFoundationConfig.leavesChunkLoadingToFtbChunks()` (option on and `ModList.get().isLoaded(FTB_CHUNKS)`), with the FTB Chunks mod id as `ServusFoundationConfig.FTB_CHUNKS`; the anchor and `SiteWakes` both call it. Existing config files keep the value they hold; Servus is unreleased, so no migration.
- **Wording.** The comment says what `true` does: anchors stay inactive and point players to FTB Chunks, warehouse sites do not wake. The lang text for the config screen becomes "Ender Anchor stays inactive with FTB Chunks", so the title reads correctly for both values.
- **Setting the option in a test.** The test never touches the global config. The anchor reads the rule through a `BooleanSupplier` that defaults to `ServusFoundationConfig::leavesChunkLoadingToFtbChunks`; the package private `respectFtbChunksAs(boolean)` swaps it for `leavesChunkLoadingToFtbChunks(respects)` on that one anchor, so the mod check still runs and nothing has to be restored on any exit path.
- **Batches.** Vanilla 26.1 groups GameTests into batches by their test environment (`GameTestBatchFactory.divideIntoBatches` groups by `GameTestInstance.batch()`, the environment holder) and runs the batches one after another. `ServusGameTest` gains `batch()` (default `"default"`); `ServusGameTests` registers one environment per batch name, `<mod>:<batch>`, through `RegisterGameTestsEvent.registerEnvironment` with no definitions (an empty `AllOf`). The option test needs no batch of its own since it changes no shared state.
- **Without FTB Chunks.** When FTB Chunks is not loaded (the Foundation run), the same test checks that the option alone changes nothing: the anchor is active. It never skips, so both runs exercise the option.
- **Where the test lives.** `AnchorGameTests.anchorStaysInactiveForFtbChunks`, using the existing `anchor(...)` helper. It checks the status, that no ticket is held and the team load.

## Risks

- None from the test: a failing or timed out test leaves no option behind, since only its own anchor sees the option on.
