## ADDED Requirements

### Requirement: Export restock runs
Every `fluid_check_interval` ticks an online warehouse SHALL look at each locked tank of its Export gantries with a keep level above 0. Its have SHALL be its amount plus what tanker runs queued or under way will bring to it. When the have is below the keep level, no run for that tank is queued or under way, the warehouse has a Tanker shuttle that reaches the gantry, and Storage gantry tanks hold the fluid, the warehouse SHALL queue one tanker run that takes the least of the missing amount, the shuttle's fluid capacity and what the chosen source tank holds beyond what runs already on their way will take from it. Runs below `tanker_min_load` SHALL wait unless they fill the missing amount completely.

#### Scenario: Pipe drains, tanker restocks
- **WHEN** an Export gantry tank locked to water keeps 8 B, holds 2 B and a Storage gantry holds 20 B of water
- **THEN** a Tanker shuttle brings water from the Storage gantry until the Export tank holds 8 B

#### Scenario: Runs on the way count
- **WHEN** a run bringing 4 B of water to an Export tank is under way and the tank holds 2 B of a keep of 8 B
- **THEN** the next run brings at most 2 B

### Requirement: Source tank for restock
The source of a restock run SHALL be the Storage gantry tank holding the fluid that the Tanker reaches, preferring the one with the most free fluid and then the shortest way. Import gantries SHALL NOT be sources of restock runs. When no Storage tank holds the fluid, no run SHALL be queued, the Export gantry's activity lamp SHALL be red and its GUI state line SHALL name the fluid that is missing from storage; when stock appears, runs SHALL start again by themselves.

#### Scenario: Nothing in stock
- **WHEN** an Export gantry keeps 4 B of Mercury and no Storage tank holds Mercury
- **THEN** no tanker drives, the Export gantry's lamp is red and its GUI says Mercury is missing

### Requirement: Restock run
A restock run SHALL drive to the rail under the source gantry, take the run's fluid (as much as the source still holds up to the run's amount), drive to the Export gantry and put it into the kept tank as far as it has room. Fluid that does not fit SHALL go back to a Storage tank by the target order of fluid put-aways, and what still does not fit SHALL be lost. Then the shuttle SHALL go home. Removal, rail loss, garage loss and recall SHALL end a restock run like a fluid put-away run.

#### Scenario: Kept tank filled meanwhile
- **WHEN** a restock run arrives with 4 B but a player meanwhile poured 3 B into the Export tank by bucket
- **THEN** the tanker puts in what fits and brings the rest back to storage
