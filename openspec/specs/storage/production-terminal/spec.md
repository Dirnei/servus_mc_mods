# storage/production-terminal Specification

## Purpose
The Production Terminal is the production overview on the data line: it shows what the producers of the line can make and what the Crafting CPUs of the line are making, and lets a player cancel an order.

## Requirements

### Requirement: Terminal on the data line
The Production Terminal SHALL be a horizontal facing block that holds no items and joins the data line it touches as a member with the link role. It SHALL show the online lamp while a formed Crafting CPU or an online warehouse is reachable on that line, checked regularly, and the dark lamp otherwise. Right click with an empty hand SHALL open its screen. The carrot SHALL turn it and the sneak carrot SHALL dismantle it; it SHALL drop itself when broken and be craftable.

#### Scenario: Lamp
- **WHEN** a Production Terminal touches a data duct whose line has a formed Crafting CPU
- **THEN** the duct shows an arm toward the terminal, the network lists it as a link member and its lamp is lit

#### Scenario: Alone
- **WHEN** a Production Terminal touches no data line
- **THEN** its lamp is dark and its screen says that no CPU or warehouse is on the line

### Requirement: Tab bar
The screen SHALL show a tab bar with a Crafting tab, a Patterns tab and a Machines tab, the Crafting tab selected when the screen opens. Clicking a tab SHALL show its content. The bar SHALL leave room for further tabs.

#### Scenario: Switch tab
- **WHEN** the player clicks the Machines tab
- **THEN** the screen shows the status of the producers of the line

#### Scenario: Patterns tab
- **WHEN** the player clicks the Patterns tab
- **THEN** the screen shows the patterns of the line and the capacity bar

### Requirement: Cancel from the terminal
Clicking the cancel button of an open job in the Crafting tab SHALL cancel it on its CPU like any cancel: steps not yet fed are dropped and their reservations released; a linked request keeps its missing amount. Done and cancelled jobs SHALL show no cancel button.

#### Scenario: Cancel
- **WHEN** a player clicks the cancel button of an open sticks job in the Crafting tab
- **THEN** the order is cancelled on its CPU and its reserved planks are available again

### Requirement: Live view
The terminal SHALL send its full content when the game opens the screen, without waiting for the periodic update. The client SHALL keep the last content of each terminal for the session and SHALL show it at once when that terminal is opened again, replacing it when the fresh content arrives; the screen SHALL show a loading hint only while it has no content for that terminal yet. While the screen is open the terminal SHALL send the current CPUs, jobs, machine states, patterns and pattern capacity every half second when something changed, and nothing when nothing changed. The content SHALL carry no recipe lists, only how many items a member can make.

#### Scenario: Step progresses
- **WHEN** the sticks step changes from getting its ingredients to crafting while the screen is open
- **THEN** the screen shows the crafting step within half a second

#### Scenario: Pattern added elsewhere
- **WHEN** another player saves a pattern from a second Production Terminal on the same line while the screen is open
- **THEN** the Patterns tab lists it within half a second

#### Scenario: Instant content
- **WHEN** the player opens the terminal
- **THEN** its content is sent in the same tick the screen opens

#### Scenario: Opened again
- **WHEN** the player closes the terminal and opens it again in the same session
- **THEN** the last content shows at once without a loading hint and is replaced by the fresh content

### Requirement: Patterns tab
The Patterns tab SHALL show a capacity bar with the used and total pattern places of the line, a plus button, and every pattern of the line with its result, its inputs and the position of its Pattern Storage. Plus SHALL open the recipe editor with an empty grid; clicking a pattern SHALL open the editor prefilled with its template and offer Delete. While the editor is open the player inventory SHALL be shown so items can be put into the ghost grid, and the rest of the screen SHALL be dimmed and inert.

#### Scenario: Capacity bar
- **WHEN** the line has one Pattern Storage with 9 places holding 2 patterns
- **THEN** the bar reads 2 of 9

#### Scenario: Edit a pattern
- **WHEN** the player clicks the sticks pattern, changes the grid to a torch layout and presses Save
- **THEN** that pattern now holds the torch recipe

### Requirement: Recipe viewer transfer into the terminal
With JEI installed, the transfer button on a vanilla crafting recipe SHALL open the editor of the open Production Terminal prefilled with that recipe's layout. Without JEI the terminal SHALL work unchanged.

#### Scenario: Plus button in JEI
- **WHEN** the terminal screen is open and the player presses the transfer button on the stick recipe in JEI
- **THEN** the editor opens with a plank above a plank and the preview shows four sticks

### Requirement: Screen layout
The Production Terminal screen SHALL show the title on its own row, the tab bar on the row below it and the list below the tab bar, so the title, the tabs and the rows never overlap; the parts of a job row (icon, name, progress bar, step text, cancel button) and of a machine row (icon, name, state, upkeep bar) SHALL not overlap and SHALL fit the list width; the pattern editor SHALL cover the list area with the player inventory below it, inside the panel. The panel SHALL fit a 1080p screen at the automatic GUI scale.

#### Scenario: Title and tabs
- **WHEN** the screen opens with all three tabs
- **THEN** the title and the tabs do not overlap

#### Scenario: Job row
- **WHEN** a job row shows a long item name and a long step text
- **THEN** the texts are cut short before the progress bar and the cancel button

### Requirement: Crafting tab
The Crafting tab SHALL list every job of the formed Crafting CPUs of the line, open jobs first and then oldest first, one row each with the item icon, the amount and item name, a progress bar of the items made against the amount, the current step in plain words and a cancel button. Above the jobs a one line header SHALL count the CPUs and their slots in use; its tooltip SHALL list each CPU with its slots and waiting jobs.

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

#### Scenario: Finished job fades
- **WHEN** a job is done
- **THEN** its row shows "Done" with a full bar and disappears a few seconds later

#### Scenario: No CPU
- **WHEN** the line has no formed Crafting CPU
- **THEN** the Crafting tab says that no Crafting CPU is on the line

### Requirement: Machines status tab
The Machines tab SHALL list every member of the data line with the producer or the upkeep role in one compact row with its icon, its name and its state, and an upkeep bar when it keeps upkeep. States SHALL be: crafting an item (with crafts left for a Crafting Station), idle, out of fuel when its upkeep is empty, blocked with the reason a step on it waits for, no machine for a Machine Adapter that faces nothing it knows, no recipes, and upkeep only for a member without the producer role. Rows with a problem (out of fuel, blocked, no machine, no recipes) SHALL come first. The tab SHALL NOT list recipes; the row tooltip SHALL say how many different items the member can make, or that a Crafting Station crafts any pattern. A Machine Adapter SHALL be named after the machine it faces with "(adapter)" added and show that machine's icon.

#### Scenario: Idle station
- **WHEN** a Crafting Station without a job is on the line
- **THEN** its row says idle and its tooltip says it crafts any pattern

#### Scenario: Station at work
- **WHEN** a Crafting Station has a sticks job with 2 crafts left
- **THEN** its row says crafting Stick, 2 left

#### Scenario: Furnace adapter
- **WHEN** a Machine Adapter faces a furnace holding 12 coal with an upkeep level of 16
- **THEN** the row reads "Furnace (adapter)" with an upkeep bar at 12 of 16, and its tooltip gives the number of items it can smelt instead of a recipe list

#### Scenario: Out of fuel first
- **WHEN** one furnace adapter has coal and another has none
- **THEN** the empty one is listed first as out of fuel

#### Scenario: Coal Generator
- **WHEN** a Coal Generator holding 9 coal with an upkeep level of 16 is on the line
- **THEN** its row says upkeep only with an upkeep bar at 9 of 16

#### Scenario: No producers
- **WHEN** the line has no producer
- **THEN** the Machines tab says that no machines are on the line
