## MODIFIED Requirements

### Requirement: Tab bar
The screen SHALL show a tab bar with a Crafting tab, a Patterns tab, a Machines tab and a CPUs tab, the Crafting tab selected when the screen opens. Clicking a tab SHALL show its content. The bar SHALL leave room for further tabs.

#### Scenario: Switch tab
- **WHEN** the player clicks the Machines tab
- **THEN** the screen shows the status of the producers of the line

#### Scenario: Patterns tab
- **WHEN** the player clicks the Patterns tab
- **THEN** the screen shows the patterns of the line and the capacity bar

#### Scenario: CPUs tab
- **WHEN** the player clicks the CPUs tab
- **THEN** the screen shows every formed Crafting CPU of the line with its slots and buffer

### Requirement: Screen layout
The Production Terminal screen SHALL show the title on its own row, the tab bar on the row below it and the list below the tab bar, so the title, the tabs and the rows never overlap; the parts of a job row (icon, name, progress bar, step text, cancel button), of a buffer line under a job, of a machine row (icon, name, state, upkeep bar) and of a CPU row (icon, name, slot bar, buffer bar, buffered items) SHALL not overlap and SHALL fit the list width; the pattern editor SHALL cover the list area with the player inventory below it, inside the panel. The panel SHALL fit a 1080p screen at the automatic GUI scale.

#### Scenario: Title and tabs
- **WHEN** the screen opens with all four tabs
- **THEN** the title and the tabs do not overlap and every tab label is readable

#### Scenario: Job row
- **WHEN** a job row shows a long item name and a long step text
- **THEN** the texts are cut short before the progress bar and the cancel button

#### Scenario: CPU row
- **WHEN** a CPU row shows a CPU with many different buffered items
- **THEN** the items that do not fit the row are cut short with a count of the rest and listed in full in the tooltip

### Requirement: Crafting tab
The Crafting tab SHALL list every job of the formed Crafting CPUs of the line, open jobs first and then oldest first, one row each with the item icon, the amount and item name, a progress bar of the items made against the amount, the current step in plain words and a cancel button. Under an open job whose items wait in its CPU's buffer, one line per later step SHALL name the step's machine and the buffered items with their counts. A job whose outputs went to storage because the buffer was full SHALL say so under its row with the items and counts. Above the jobs a one line header SHALL count the CPUs and their slots in use; its tooltip SHALL list each CPU with its slots and waiting jobs.

#### Scenario: Running job
- **WHEN** a CPU on the line runs a job for 8 sticks whose step on a Crafting Station has made 4 sticks of 2 crafts
- **THEN** the row shows 8 Stick, a half full progress bar and "Crafting Station: crafting 1 of 2"

#### Scenario: Missing ingredient
- **WHEN** a job waits because 3 Oak Planks are missing
- **THEN** its row says "Waiting: 3 Oak Planks missing"

#### Scenario: Waiting for a CPU slot
- **WHEN** a job waits for a free CPU slot
- **THEN** its row says "Waiting for a CPU"

#### Scenario: Out of fuel
- **WHEN** a job's step runs on a furnace adapter whose furnace has no upkeep left
- **THEN** its row says "Waiting: Furnace (adapter) out of fuel"

#### Scenario: Header
- **WHEN** the line has two CPUs with 2 slots each and one open job
- **THEN** the header reads "2 CPUs, 1 of 4 slots"

#### Scenario: Buffered planks
- **WHEN** a crafting table job holds 4 Oak Planks in its CPU's buffer for its crafting table step on a Crafting Station
- **THEN** a line under the job reads "In CPU buffer for Crafting Station: 4 Oak Planks"

#### Scenario: Buffer overflowed
- **WHEN** 16 Oak Planks of a job went to storage because its CPU's buffer was full
- **THEN** a line under the job reads "Buffer full: 16 Oak Planks went to storage"

#### Scenario: Finished job fades
- **WHEN** a job is done
- **THEN** its row shows "Done" with a full bar and disappears a few seconds later

#### Scenario: No CPU
- **WHEN** the line has no formed Crafting CPU
- **THEN** the Crafting tab says that no Crafting CPU is on the line

## ADDED Requirements

### Requirement: CPUs tab
The CPUs tab SHALL list every formed Crafting CPU of the line, lowest position first, one row each with the CPU block icon, its position and box size, a bar of order slots in use against its slots with the number of waiting jobs, a bar of buffer stacks in use against its room, and the buffered items with their counts. The row tooltip SHALL list every buffered item with its count and the job it belongs to. Leftovers of done or cancelled jobs that wait to leave SHALL be shown as leaving.

#### Scenario: CPU with buffer
- **WHEN** a 3x2x2 CPU with 4 slots and 8 stacks of room runs 2 jobs and holds 4 Oak Planks and 2 Iron Ingots
- **THEN** its row shows 3x2x2, a slot bar at 2 of 4, a buffer bar at 2 of 8 and the planks and ingots with their counts

#### Scenario: Waiting jobs
- **WHEN** a CPU with 2 slots holds 3 open jobs
- **THEN** its slot bar is full and the row says 1 waiting

#### Scenario: No buffer
- **WHEN** a CPU has no Buffer blocks
- **THEN** its row shows no buffer bar and says it has no buffer

#### Scenario: No CPU
- **WHEN** the line has no formed Crafting CPU
- **THEN** the CPUs tab says that no Crafting CPU is on the line
