# Proposal: m9-anchor-ftb-chunks

## Why

With FTB Chunks installed every Servus Ender Anchor stays inactive by default and points players to FTB Chunks. Packs such as ATM11 ship FTB Chunks, so the anchor, its Anchor Upgrades and the team cap are dead content there, and the nine anchor GameTests fail in every run that loads FTB Chunks (the Alchemy GameTest run). The user decided that the anchor works normally next to FTB Chunks by default; pack authors who want FTB Chunks to be the only chunk loader can still switch anchors off.

## What Changes

- The server config `enderAnchorRespectsFtbChunks` defaults to `false`. With FTB Chunks installed, anchors then load their square like without it.
- The config comment and its lang text say that `true` keeps Ender Anchors inactive (and warehouse sites asleep) in favour of FTB Chunks.
- With the option on and FTB Chunks installed, behaviour stays as today: the anchor stays inactive with the status "Inactive: use FTB Chunks".
- A GameTest covers the option on, in its own GameTest batch so no other test sees the changed value. The Servus GameTest annotation gains a batch name for that.
- The docs pages for the Ender Anchor and the warehouse site wake describe the new default.
- PLAN.md gains an open point: the anchor respects FTB Chunks claims (loads only chunks the owner's team has claimed or unclaimed ones), as a soft integration with the FTB Chunks API.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/ender-anchor`: the requirement "FTB Chunks respected" changes from on by default to off by default; with the option on, anchors stay inactive and point to FTB Chunks.

## Impact

- `foundation/.../config/ServusFoundationConfig.java`: default, comment, an in-memory setter for GameTests.
- `foundation/.../datagen/MachineDataParts.java` and the generated `en_us.json`: config lang text.
- `foundation/.../gametest/ServusGameTest.java`, `ServusGameTests.java`: optional batch name, one test environment per batch.
- `foundation/.../machine/anchor/AnchorGameTests.java`: the new test.
- `shuttles/.../site/SiteWakes.java` reads the same option, so site wakes follow the new default too; its spec already says "while ... is on" and needs no change.
- Docs: `docs/src/content/docs/mods/foundation.md`, `docs/src/content/docs/mods/shuttles.md`.
- `PLAN.md`: new open point under "Open: Foundation".
