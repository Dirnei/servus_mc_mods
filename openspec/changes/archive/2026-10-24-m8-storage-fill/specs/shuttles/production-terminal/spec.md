## ADDED Requirements

### Requirement: Storage tab
The Storage tab SHALL list every warehouse the terminal reaches on its line, by site name. Each warehouse SHALL have a header row with its name, its rack slots in use and total and a bar, then one row per zone as the storage fill orders them (full first, then the fullest), each with its slots and a bar, then a Fluids row with the mB in its Storage gantries and their capacity and a bar. Full rows SHALL use the alert colour. A warehouse without racks SHALL show a "no racks" row instead of zone rows; one without Storage gantries SHALL show no Fluids row. The header tooltip SHALL name the number of rack bays and Storage gantries. Without a reachable warehouse the tab SHALL say that no warehouse is on the line. The tab SHALL follow the counts while open.

#### Scenario: One warehouse with zones
- **WHEN** a terminal reaches one warehouse with the zones Ores (81 of 81) and Food (30 of 81) and unzoned racks (301 of 567) and the player opens the Storage tab
- **THEN** the tab shows the warehouse header with 412 / 729, then Ores in the alert colour, (no zone), Food, and a Fluids row when it has Storage gantries

#### Scenario: Two warehouses
- **WHEN** the line reaches the base and an outpost warehouse
- **THEN** the tab shows two headers, each followed by its own zone and fluid rows

#### Scenario: No warehouse
- **WHEN** the line has a CPU but no warehouse
- **THEN** the Storage tab says that no warehouse is on the line

## MODIFIED Requirements

### Requirement: Tab bar
The screen SHALL show a tab bar with a Crafting tab, a Patterns tab, a Machines tab, a CPUs tab and a Storage tab, the Crafting tab selected when the screen opens. Clicking a tab SHALL show its content. The bar SHALL leave room for further tabs.

#### Scenario: Switch tab
- **WHEN** the player clicks the Machines tab
- **THEN** the screen shows the status of the producers of the line

#### Scenario: Patterns tab
- **WHEN** the player clicks the Patterns tab
- **THEN** the screen shows the patterns of the line and the capacity bar

#### Scenario: CPUs tab
- **WHEN** the player clicks the CPUs tab
- **THEN** the screen shows every formed Crafting CPU of the line with its slots and buffer

#### Scenario: Storage tab
- **WHEN** the player clicks the Storage tab
- **THEN** the screen shows the storage fill of every warehouse on the line

### Requirement: Screen layout
The Production Terminal screen SHALL show the title on its own row, the tab bar on the row below it and the list below the tab bar, so the title, the tabs and the rows never overlap; each tab SHALL be as wide as its label plus an even share of the row's spare width, so all tabs fit the row; the parts of a job row (icon, name, progress bar, step text, cancel button), of a buffer line under a job, of a machine row (icon, name, state, upkeep bar), of a CPU row (icon, name, slot bar, buffer bar, buffered items) and of a storage row (name, numbers, bar) SHALL not overlap and SHALL fit the list width; the pattern editor SHALL cover the list area with the player inventory below it, inside the panel. The panel SHALL fit a 1080p screen at the automatic GUI scale.

#### Scenario: Title and tabs
- **WHEN** the screen opens with all five tabs
- **THEN** the title and the tabs do not overlap, the tabs fit the row, and every tab label is readable

#### Scenario: Job row
- **WHEN** a job row shows a long item name and a long step text
- **THEN** the texts are cut short before the progress bar and the cancel button

#### Scenario: CPU row
- **WHEN** a CPU row shows a CPU with many different buffered items
- **THEN** the items that do not fit the row are cut short with a count of the rest and listed in full in the tooltip

#### Scenario: Storage row
- **WHEN** a zone row shows a long zone name
- **THEN** the name is cut short before the numbers and the bar
