# Proposal

## Why

The Tier 1 numbers were chosen so that one specific layout produces one Red Pack every 5 seconds. A GameTest that builds that layout and measures the rate protects the balance against every later change. PLAN.md milestone M2, item "GameTest: the reference balanced block produces 1 Red Pack per 5 s". It is also the first GameTest of the family and sets up the plumbing.

## What Changes

- GameTest plumbing in Manufacturing: an empty structure template, a `@GameTestHolder` class registered through `RegisterGameTestsEvent`.
- `ReferenceBlockGameTest`: builds the reference block from PLAN.md (two iron extractor and smelter pairs, one copper pair, one coal extractor, three assemblers with gear, wire and pack recipes, one lab, generators, ducts), pre fills fuel, runs 60 seconds and asserts the lab received between 7 and 13 red packs, which is the 1 per 5 s steady state after the pipeline fills.

## Capabilities

### New Capabilities
- `manufacturing/tier1-balance`: the throughput promise of the reference block.

### Modified Capabilities
None.

## Non-goals

- Testing research consumption in the GameTest (no player, no team). The lab only collects packs here.
- More GameTests for single machines; the RCON checks of earlier changes cover them for now.

## Impact

- `gradlew :manufacturing:runGameTestServer` now runs one real test and exits non zero on failure.
- A structure file under `data/servus_manufacturing/structure/`.
