# docs/balance-lab Specification

## Purpose

The documentation site gives players and modpack authors the Servus docs, and gives the developers a Balance lab to inspect production chains, try balance changes and draft new content in the browser, then export the result as a proposal for implementation.

## Requirements

### Requirement: Site is published to GitHub Pages
The site SHALL be built from `docs/` and deployed to GitHub Pages on every push to `main` that touches `docs/`, and on manual dispatch. The deploy SHALL run the site's unit tests first and SHALL NOT deploy when they fail.

#### Scenario: Docs change on main
- **WHEN** a commit on `main` changes a file under `docs/`
- **THEN** the docs workflow tests, builds and publishes the site

#### Scenario: Failing ratio test
- **WHEN** a ratio unit test fails
- **THEN** the workflow stops and the published site stays unchanged

### Requirement: Public documentation skeleton
The site SHALL have a landing page, a getting started page, an overview page per mod (Foundation, Manufacturing, Storage) and a datapack reference describing the JSON formats of machine stats, deposits, techs and Servus recipes. Public pages SHALL work without JavaScript.

#### Scenario: Datapack reference
- **WHEN** a modpack author opens the machine stats reference
- **THEN** it lists every machine stats field with its meaning and an example file

#### Scenario: Mod pages
- **WHEN** a reader opens the mods section
- **THEN** it lists Servus: Foundation, Servus: Manufacturing and Servus: Storage, and the Foundation page covers the Utility Duct, the Fluid Tank, facades, goggles and the Configuration Carrot

### Requirement: Lab shows which data it uses
Every Balance lab page SHALL show the game data schema version, the mod version and the commit the site was built from.

#### Scenario: Header
- **WHEN** the lab is built from commit `abc1234`
- **THEN** the lab header shows `abc1234` next to the mod version

### Requirement: Chain view
For a chosen product and target rate, the lab SHALL draw the production chain from deposits to the product. Each machine node SHALL show the machine, the recipe, items per second, the machine count needed (rounded up), utilization, and FE per tick or coal per second. The chain SHALL show totals for machines, FE per tick, generators needed and coal per second including the coal for fuel.

#### Scenario: Reference block
- **WHEN** the chain view shows one Red Pack per 5 seconds with default data and burner routes
- **THEN** it shows 2 extractors and 2 smelters on iron, 1 extractor and 1 smelter on copper, 1 extractor on coal, 3 assemblers and 1 research lab

### Requirement: Ratio rules follow the machines
The ratio math SHALL use the same timing rules as the game: a machine's cycle is its `work_ticks`, except where the machine takes its time from the recipe (assembling and crushing recipe time, the lab's `ticks_per_pack`). Burners use fuel only while working, one fuel item lasting its burn time divided by `fuel_divisor`. Powered machines draw FE only while working, so FE draw is weighted by utilization.

#### Scenario: Generator coal
- **WHEN** a coal generator with `fuel_divisor` 4 runs at full load
- **THEN** it uses one coal per 20 seconds (1600 ticks divided by 4)

#### Scenario: Idle assembler draws less
- **WHEN** an assembler is at 20 percent utilization with `energy_per_tick` 20
- **THEN** its average draw is 4 FE per tick

### Requirement: Route choice
Where an item can be made by more than one recipe in the data, the lab SHALL let the user pick the route per item and SHALL recompute the chain.

#### Scenario: Grit route
- **WHEN** the user switches iron ingot from smelting raw iron to crushing raw iron into grit and smelting the grit
- **THEN** the chain shows a crusher node, fewer iron extractors per ingot, and the new totals

### Requirement: Cycles and missing sources
The lab SHALL NOT loop on recipe cycles: a cycle SHALL be shown as a marked loop node. An item without any producing recipe or deposit SHALL be shown as a marked "no source" leaf.

#### Scenario: Missing source
- **WHEN** a drafted recipe needs an item no recipe or deposit produces
- **THEN** that item appears as a "no source" leaf and the totals still render

### Requirement: Research view
The lab SHALL show the tech graph with each tech's parents, pack cost, `ticks_per_pack` and unlocks, and the time to research a tech for a user given number of labs and pack supply rate.

#### Scenario: Time to research
- **WHEN** a tech costs 10 Red Packs at 100 ticks per pack, with 1 lab and packs supplied at one per 5 seconds
- **THEN** the lab shows 50 seconds

### Requirement: Edit numbers
The user SHALL be able to edit every number from the game data: machine stats, recipe times and counts, deposit yields and tech costs. Edited values SHALL be highlighted, and nodes SHALL show the change against the committed baseline, for example "3 to 2 assemblers".

#### Scenario: Faster red pack
- **WHEN** the user sets the Red Pack recipe time from 100 to 80 ticks
- **THEN** the field is highlighted and every affected count and total updates with its old value shown

### Requirement: Draft new content
The user SHALL be able to draft new items (id, name, note), new recipes of an existing recipe type using real and drafted items, and concept machines (name, kind, stats, a new recipe type they run). Drafts SHALL take part in the ratio math like real data, SHALL be drawn with a dashed outline, and concept machines SHALL be labeled "needs code".

#### Scenario: Drafted intermediate
- **WHEN** the user drafts an item "Copper Coil", a recipe making it from wire, and changes the Red Pack recipe to use it
- **THEN** the chain shows the coil node dashed and recomputes machine counts and coal

#### Scenario: Concept machine
- **WHEN** the user drafts a "Wire Drawer" running a new recipe type for copper wire
- **THEN** the wire route can be switched to it and its node is labeled "needs code"

### Requirement: Export a proposal
The lab SHALL export the current draft as a proposal file with a schema version, the baseline game data version, a title, edits as baseline and new value pairs, the new items, recipes and machines, an impact summary for the viewed chain (before and after counts, FE and coal), and free text notes per draft entry and for the whole proposal.

#### Scenario: Export
- **WHEN** the user exports a draft with one edited recipe time and one new item
- **THEN** the file holds one edit with both values, one new item, the notes and the impact summary

### Requirement: Import a proposal and detect conflicts
The lab SHALL import a proposal file and restore its draft. When an edit's baseline differs from the current game data, the lab SHALL mark it as a conflict showing the proposal's baseline, the current value and the proposed value, and SHALL NOT apply it silently. Files with an unknown schema version or invalid content SHALL be rejected with a readable message.

#### Scenario: Baseline moved
- **WHEN** a proposal edits a recipe time from 80 to 40 and the current data has 60
- **THEN** the edit shows as a conflict with 80, 60 and 40

#### Scenario: Unknown schema
- **WHEN** a proposal file has schema 2 and the site understands schema 1
- **THEN** the lab shows an error naming both versions and keeps the current draft

### Requirement: Draft persistence and share links
The current draft SHALL be saved in the browser and restored on reload. The lab SHALL create a share link that encodes the draft in the URL; opening it SHALL load that draft. When the draft is too large for a link, the lab SHALL say so and point to export.

#### Scenario: Reload
- **WHEN** the user edits a value and reloads the page
- **THEN** the edit is still there

#### Scenario: Share link
- **WHEN** another person opens a share link
- **THEN** they see the same edits and drafts
