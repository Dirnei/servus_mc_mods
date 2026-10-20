# Design: m9-anchor-ftb-chunks

## Context

`EnderAnchorBlockEntity.allowance()` returns `STATUS_FTB_CHUNKS` (3) and unregisters the anchor while `ServusFoundationConfig.enderAnchorRespectsFtbChunks()` is true and `ModList.get().isLoaded("ftbchunks")`. `SiteWakes` in Shuttles uses the same check to keep sites asleep. The option is `true` by default, so in the Alchemy GameTest run, which loads FTB Chunks as a dev runtime mod, the nine anchor GameTests (`anchorgametests.*`, `machinegametests.enderanchor*`) fail. The user decided to flip the default (option A). Respecting FTB Chunks claims (option B) becomes an open point in PLAN.md.

## Decisions

- **Default `false`.** Only the default in `ModConfigSpec.Builder.define` changes. The check in the anchor and in `SiteWakes` stays as it is. Existing config files keep the value they hold; Servus is unreleased, so no migration.
- **Wording.** The comment says what `true` does: anchors stay inactive and point players to FTB Chunks, warehouse sites do not wake. The lang text for the config screen becomes "Ender Anchor stays inactive with FTB Chunks", so the title reads correctly for both values.
- **Setting the option in a test.** `ServusFoundationConfig.setEnderAnchorRespectsFtbChunks(boolean)` calls `ModConfigSpec.BooleanValue.set`, which changes the loaded value and its cache without firing events or writing the file (NeoForge 26.1.2 `ModConfigSpec.ConfigValue.set`). The test reads the old value, sets `true`, and restores the old value in a `finally` around its checks.
- **Isolation.** Vanilla 26.1 groups GameTests into batches by their test environment (`GameTestBatchFactory.divideIntoBatches` groups by `GameTestInstance.batch()`, the environment holder) and runs the batches one after another. `ServusGameTest` gains `batch()` (default `"default"`); `ServusGameTests` registers one environment per batch name, `<mod>:<batch>`, through `RegisterGameTestsEvent.registerEnvironment` with no definitions (an empty `AllOf`). The new test uses batch `ftb_chunks_respected`, so it runs alone and no other anchor test sees the changed value.
- **Without FTB Chunks.** When FTB Chunks is not loaded (the Foundation run), the same test checks that the option alone changes nothing: the anchor is active. It never skips, so both runs exercise the option.
- **Where the test lives.** `AnchorGameTests.anchorStaysInactiveForFtbChunks`, using the existing `anchor(...)` helper. It checks the status, that no ticket is held and the team load. The FTB Chunks mod id constant in `EnderAnchorBlockEntity` becomes package visible for the test.

## Risks

- A test that fails inside the isolated batch before its check would leave the option on for the rest of the run. The check runs at a fixed delay well inside the timeout and the restore sits in a `finally`, so this needs a crash of the test itself.
