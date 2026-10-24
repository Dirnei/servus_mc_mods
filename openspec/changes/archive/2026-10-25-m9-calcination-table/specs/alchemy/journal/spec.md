## MODIFIED Requirements

### Requirement: Recipe lists from the shipped recipes
The Workshop entries of the Coagulator and Athanor SHALL list every coagulation and Athanor recipe Alchemy ships, one line per recipe in the form "Bone Meal: 60 Salt, 20 Sulfur", generated from the same data that generates the recipes, so the book always matches the shipped recipes. Formatting the lines SHALL be pure Java covered by JUnit. Calcination recipes are shown in the calcination table instead.

#### Scenario: Calcination list
- **WHEN** the player reads the Calciner entry
- **THEN** it has no text list of calcination recipes but the calcination table

#### Scenario: Coagulation list
- **WHEN** the player reads the Coagulator entry
- **THEN** a page lists "Bone Meal: 60 Salt, 20 Sulfur" among the other coagulation recipes

#### Scenario: Principles omitted when zero
- **WHEN** a recipe uses only Salt
- **THEN** its line names only Salt

## ADDED Requirements

### Requirement: Calcination table
The Calciner entry SHALL end in a table of the calcination recipes the reader's client knows, one row per recipe that burns at least one item, grouped under the headings Overworld, Nether and End in that order, with a group's rows in recipe id order and a group without rows left out. The table SHALL run over as many pages as it needs; a heading never ends a page, and a group that continues on the next page repeats its heading there.

#### Scenario: Groups in order
- **WHEN** a player pages through the table with the shipped recipes
- **THEN** the Overworld rows come first, then the Nether rows, then the End rows, each under its heading

#### Scenario: Datapack recipe
- **WHEN** a datapack adds a calcination recipe marked `nether`
- **THEN** after the reload the table has one more row under Nether

#### Scenario: Shadowed recipe
- **WHEN** more specific recipes take every item of a recipe
- **THEN** that recipe has no row

#### Scenario: Group goes on
- **WHEN** the Overworld rows do not fit on the first page
- **THEN** the next page starts with the Overworld heading again and the rest of its rows

#### Scenario: Table longer than its pages
- **WHEN** datapacks add more rows than the entry's table pages hold
- **THEN** the last table page ends with a line saying how many rows are left out and that JEI lists them

### Requirement: Rows reveal by calcining
A row of the calcination table SHALL show the recipe's item, its name and the mB of Salt, Sulfur and Mercury it gives once the reader's team has burned anything with that recipe in a Calciner; until then it SHALL show a question mark in place of the item, no name and no amounts, and its hint. JEI SHALL show every recipe whatever the team has tried.

#### Scenario: First Nether Wart
- **WHEN** a team's Calciner burns its first Nether Wart while a member reads the table
- **THEN** the Nether Wart row shows the item, "Nether Wart" and 60 Sulfur and 20 Mercury

#### Scenario: Not yet tried
- **WHEN** a new team opens the table
- **THEN** every row shows a question mark and a hint, and no item, name or amount

#### Scenario: JEI unchanged
- **WHEN** a player whose team has tried nothing looks up Blaze Rods in JEI
- **THEN** JEI shows the Blaze Rod calcination recipe with its amounts

### Requirement: Tried recipes per team
The recipes a team has tried SHALL be kept per team in world data and survive a restart. A newly tried recipe SHALL reach every online member of the team at once, also while the book is open.

#### Scenario: Other team
- **WHEN** team A has burned Ender Pearls and team B has not
- **THEN** a member of team B sees the Ender Pearl row still hidden

#### Scenario: Restart
- **WHEN** a team has tried Netherrack and the server restarts
- **THEN** the Netherrack row is still revealed for the team

#### Scenario: Teammate's calcine
- **WHEN** a teammate's Calciner burns the first Ender Pearl while another member reads the table
- **THEN** that member sees the Ender Pearl row revealed

### Requirement: Hints for untried rows
The hint of a row SHALL be the lang key `journal.servus_alchemy.calcination.<recipe namespace>.<recipe name>.hint`, where the recipe name is the recipe path without a leading `calcination/` and with `/` as `.`; a recipe without that key SHALL show a generic "Not yet tried" text. Every shipped calcination recipe SHALL have a short English hint in Paracelsus' voice that points at what to burn without naming it. Hints SHALL live in lang files only.

#### Scenario: Shipped hint
- **WHEN** a new team reads the row of `servus_alchemy:calcination/blaze_rod`
- **THEN** it shows the text of `journal.servus_alchemy.calcination.servus_alchemy.blaze_rod.hint`, which does not say "Blaze Rod"

#### Scenario: Datapack recipe without a hint
- **WHEN** a datapack adds `mypack:calcination/ash_wood` without a lang key for it
- **THEN** its hidden row shows "Not yet tried"
