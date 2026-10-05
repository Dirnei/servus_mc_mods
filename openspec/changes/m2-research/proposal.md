# Proposal

## Why

Research is the progression spine of Manufacturing: packs go into a lab, techs unlock machines and recipes. PLAN.md milestone M2, items "Research Lab, tech tree data format, team progress, first five techs" and "Minimal tech tree screen". After this change the Tier 1 loop closes: ore, ingots, gears, wire, red packs, research.

## What Changes

- Pure Java `ResearchState` in Manufacturing: completed techs, current research, per tech consumed packs, availability through parents. JUnit tested.
- `TeamProvider` in Servus Lib: who shares progress. Default is the player itself; FTB Teams can plug in later.
- Tech definitions as a synced datapack registry `servus_manufacturing:tech` (`data/<ns>/servus_manufacturing/tech/<name>.json`): icon, parents, pack cost, ticks per pack, unlocks. Five Tier 1 techs ship as data.
- Team progress stored in world saved data and synced to the team's players with a custom payload; a second payload lets a player pick the research target.
- **Research Lab**: a powered machine (10 FE/t) owned by its placer's team. It consumes one required pack per period for the team's current research. Several labs work in parallel.
- Lab GUI with pack slots, progress, energy and a button opening the **tech tree screen**: a list of techs with status, cost and progress; clicking an available tech starts it.
- Assembler recipe gating: recipes with a `tech` field are shown locked until the tech is complete and cannot be selected.

## Capabilities

### New Capabilities
- `lib/teams`: who shares progress and inventories in the Servus family.
- `manufacturing/research`: tech data, team progress, the lab and the tech tree screen.

### Modified Capabilities
- `manufacturing/assembler`: adds research gating of recipes.

## Non-goals

Deferred to later changes:
- The content the five techs unlock (Electric Extractor, Crusher, Splitter and Filter Ducts, Stone Processing, Logistics) arrives in M3. Unlock lists already name the planned recipe ids.
- FTB Teams integration (the provider hook exists).
- A graphical tree with lines; the screen is a list ordered by depth.
- Chunk loading for overnight research.

## Impact

- First custom network payloads and first saved data in the family.
- First datapack registry; its JSON lives under `data/servus_manufacturing/servus_manufacturing/tech/`.
- `MachineBlock` records the placer for owned machines.
