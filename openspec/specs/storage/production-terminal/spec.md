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
The screen SHALL show a tab bar with an Orders tab, a Machines tab and a Patterns tab, the Orders tab selected when the screen opens. Clicking a tab SHALL show its content. The bar SHALL leave room for further tabs.

#### Scenario: Switch tab
- **WHEN** the player clicks the Machines tab
- **THEN** the screen shows the producers of the line

#### Scenario: Patterns tab
- **WHEN** the player clicks the Patterns tab
- **THEN** the screen shows the patterns of the line and the capacity bar

### Requirement: Machines tab
The Machines tab SHALL list every member of the data line with the producer or the upkeep role with its block name and its state: working on an item when the producer reports an active recipe, idle when it has recipes and none runs, no recipes when it has none. A Crafting Station SHALL instead show crafting an item with the crafts left while it has a job, or idle, and one line saying it crafts any pattern. A Machine Adapter SHALL be named after the machine it faces with "(adapter)" added. Every member with upkeep SHALL show its upkeep item with the count it holds and the level. A member with upkeep but without the producer role SHALL show the state "upkeep only" instead of a recipe state. Under each other producer it SHALL list the recipes it reports as outputs from inputs with their time. The list SHALL scroll with the mouse wheel when it does not fit.

#### Scenario: Crafting Station with sticks
- **WHEN** a Crafting Station without a job is on a line that has a sticks pattern
- **THEN** the Machines tab lists the Crafting Station as idle and says it crafts any pattern, with no recipe list

#### Scenario: Crafting Station at work
- **WHEN** a Crafting Station has a sticks job with 2 crafts left
- **THEN** the Machines tab lists it as crafting sticks, 2 left

#### Scenario: No producers
- **WHEN** the line has no producer
- **THEN** the Machines tab says that no machines are on the line

#### Scenario: Adapter on a furnace
- **WHEN** a Machine Adapter faces a furnace holding 12 coal with an upkeep level of 16
- **THEN** the Machines tab lists "Furnace (adapter)" with upkeep Coal 12 of 16 and its smelting recipes

#### Scenario: Coal Generator
- **WHEN** a Coal Generator holding 9 coal with an upkeep level of 16 is on the line
- **THEN** the Machines tab lists Coal Generator as upkeep only with upkeep Coal 9 of 16

#### Scenario: Burner Smelter
- **WHEN** a Burner Smelter is on the line
- **THEN** the Machines tab lists it with its state, its upkeep and its smelting recipes

### Requirement: Orders tab
The Orders tab SHALL list every formed Crafting CPU of the line, lowest position first, with how many of its order slots are in use, its slot count and how many of its orders wait. Under each CPU it SHALL list its orders with id, item, amount, state, blocked reason and source (request, stock rule or manual), and under each order its steps with the producer's name, the step output, its batches, its state and its reason. The list SHALL scroll with the mouse wheel when it does not fit.

#### Scenario: Running order
- **WHEN** a CPU on the line runs a request's order for 8 sticks with one step on a Crafting Station
- **THEN** the Orders tab shows the CPU with 1 of 2 slots in use and 0 waiting, the order for 8 sticks from a request, and its sticks step on the Crafting Station with its state

#### Scenario: No CPU
- **WHEN** the line has no formed Crafting CPU
- **THEN** the Orders tab says that no Crafting CPU is on the line

### Requirement: Cancel from the terminal
Clicking an open order in the Orders tab SHALL cancel it on its CPU like any cancel: steps not yet fed are dropped and their reservations released; a linked request keeps its missing amount. Done and cancelled orders SHALL NOT react to clicks.

#### Scenario: Cancel
- **WHEN** a player clicks an open sticks order in the Orders tab
- **THEN** the order is cancelled on its CPU and its reserved planks are available again

### Requirement: Live view
While the screen is open the terminal SHALL send the current CPUs, machines, orders, patterns and pattern capacity every half second when something changed, and nothing when nothing changed.

#### Scenario: Step progresses
- **WHEN** the sticks step changes from delivering to producing while the screen is open
- **THEN** the screen shows producing within half a second

#### Scenario: Pattern added elsewhere
- **WHEN** another player saves a pattern from a second Production Terminal on the same line while the screen is open
- **THEN** the Patterns tab lists it within half a second

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
