# Design

## Context

`MachineBlockEntity` fixes every machine's front with `SideConfig.lock(RelativeSide.FRONT, SideMode.DISABLED)` in its constructor, and the pumps add `lock(RelativeSide.BOTTOM, SideMode.DISABLED)` in theirs. The Lib side config already greys fixed sides in the panel, ignores the carrot on them, skips their markers and loads them as the fixed mode (`lib/side-config` requirement "Fixed sides").

## Goals / Non-Goals

**Goals:** close the extractors' bottom with the existing mechanism.

**Non-Goals:** any change to Lib or to other machines.

## Decisions

### D1. Lock in the extractor constructors
`BurnerExtractorBlockEntity` and `ElectricExtractorBlockEntity` call `sideConfig().lock(RelativeSide.BOTTOM, SideMode.DISABLED)` after the super constructor, the same line the pumps use. The FE capability stays registered on every face, as for all powered machines.
Alternative: a flag on `MachineBlockEntity` for "stands on a source". Rejected for two blocks plus two pumps; one line each is clearer.

### Pure Java vs Minecraft
No new pure logic (`SideConfig.lock` is covered by `SideConfigTest`). GameTest: an extractor's bottom stays Off after cycling it, and a hopper below gets no output.

## Risks / Trade-offs

- [Worlds with a configured bottom] → Lock wins on load; nothing is released, so no migration.
