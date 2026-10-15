# Spec Delta

## MODIFIED Requirements

### Requirement: Per side item filters
Each side of a duct's item line SHALL have a whitelist of up to nine ghost items. A slot SHALL be set by dragging an item from JEI onto it, by picking an item in the search popup, by clicking it with an item on the cursor or by shift clicking an inventory item; right clicking a slot SHALL clear it. Setting a slot SHALL never take the item from the player. An empty list passes everything. Items pulled in from a side and items pushed out to a side SHALL match that side's list.

#### Scenario: Sorting pull
- **WHEN** a chest holding cobblestone and dirt touches a pull in side whose filter lists cobblestone
- **THEN** only the cobblestone leaves the chest

#### Scenario: Sorting delivery
- **WHEN** chest A's side lists iron ingots and chest B's side has no filter and iron and copper ingots arrive
- **THEN** chest A receives only iron ingots and chest B receives the copper ingots

#### Scenario: Filter for an item the player does not own
- **WHEN** a player with an empty inventory opens the endpoint GUI, clicks the first filter slot, types "diamond" and picks Diamond
- **THEN** the slot shows a diamond, the side only passes diamonds, and the player's inventory is still empty

#### Scenario: Drag from JEI
- **WHEN** JEI is installed and the player drags Iron Ingot from the JEI list onto the third filter slot
- **THEN** the third slot shows an iron ingot and the side passes iron ingots

#### Scenario: Clear by right click
- **WHEN** the player right clicks a filter slot that shows cobblestone
- **THEN** the slot is empty

#### Scenario: Cursor item is kept
- **WHEN** the player clicks a filter slot while holding a stack of 32 cobblestone on the cursor
- **THEN** the slot shows cobblestone and the cursor still holds 32 cobblestone

### Requirement: Duct GUI
Using the Configuration Carrot, not sneaking, on an arm of a duct that touches an inventory or machine SHALL open the endpoint GUI for that side. It SHALL show the side and the touched block's name, one tab per present line on its left edge showing the line's item, and the page of the selected tab: the line's mode button, the eight channel cells with the current channel framed, and a remove button; the Item Pipe page also the filter slots and the priority with minus and plus buttons; the data line page only an on and off button and the remove button. The player inventory SHALL be shown under every page. Filter slots SHALL be hidden and inert on every page but the Item Pipe page. Clicking a filter slot with an empty cursor SHALL open a search popup over the GUI that lists every item with its icon and name, filtered by the typed text, with a "none" entry first; picking an entry SHALL set the slot, "none" SHALL clear it. Using the carrot on the centre or on an arm toward another duct SHALL open nothing. Bare hand right click SHALL open nothing.

#### Scenario: Configure one side
- **WHEN** the player uses the carrot on the west arm that touches a chest, sets the item mode to pull in and places cobblestone in a filter slot
- **THEN** the duct pulls only cobblestone from the west

#### Scenario: Two lines, two tabs
- **WHEN** the player opens the GUI of a duct with an Item Pipe and a Power Cable
- **THEN** two tabs show, the Item Pipe page is selected, and clicking the Power Cable tab shows its mode, channels and remove button without filter slots

#### Scenario: Fluid Pipe tab
- **WHEN** the player opens the GUI of an arm whose duct carries all three lines and that touches a tank
- **THEN** three tabs show, and the Fluid Pipe page shows its mode, channels and remove button without filter slots or priority

#### Scenario: Data tab
- **WHEN** the player opens the GUI of an arm toward a Warehouse Controller on a duct with the data line
- **THEN** the data page shows an on and off button and the remove button, without channels, filter slots or priority

#### Scenario: Duct to duct arm
- **WHEN** the player uses the carrot on an arm that leads to another duct
- **THEN** no GUI opens

#### Scenario: Bare hand
- **WHEN** the player right clicks a duct without the data line with an empty hand
- **THEN** no GUI opens and the click passes through

#### Scenario: Search popup
- **WHEN** the player clicks an empty filter slot with an empty cursor
- **THEN** a popup with a search field and an item list opens over the GUI; typing narrows the list, Escape closes it without changing the slot

#### Scenario: Popup without JEI
- **WHEN** JEI is not installed and the player clicks a filter slot with an empty cursor
- **THEN** the search popup opens and works the same way
