# Spec Delta

## ADDED Requirements

### Requirement: Per side item filters
Each side of a duct's item channel SHALL have a whitelist of up to nine ghost items, set in the duct GUI from the cursor or by shift clicking an inventory item, cleared by clicking with an empty cursor. An empty list passes everything. Items pulled in from a side and items pushed out to a side SHALL match that side's list.

#### Scenario: Sorting pull
- **WHEN** a chest holding cobblestone and dirt touches a pull in side whose filter lists cobblestone
- **THEN** only the cobblestone leaves the chest

#### Scenario: Sorting delivery
- **WHEN** chest A's side lists iron ingots and chest B's side has no filter and iron and copper ingots arrive
- **THEN** chest A receives only iron ingots and chest B receives the copper ingots

### Requirement: Per side priorities
Each side of a duct's item channel SHALL have a priority from -9 to 9, default 0, set in the duct GUI. Routing SHALL offer an item to all outputs of the highest priority first, round robin among them, and only move to lower priorities when none of them accepts.

#### Scenario: Preferred output
- **WHEN** two chests touch push out sides with priorities 5 and 0 and 20 items arrive
- **THEN** all 20 land in the priority 5 chest

#### Scenario: Overflow
- **WHEN** the priority 5 chest is full
- **THEN** further items go to the priority 0 chest

### Requirement: Duct GUI
Right clicking a duct SHALL open a menu with a selector for the six sides and, for the selected side, the mode button of each present channel, the filter slots, the priority with minus and plus buttons, a remove button per channel and the player inventory.

#### Scenario: Configure one side
- **WHEN** the player selects the west side, sets the item mode to pull in and places cobblestone in a filter slot
- **THEN** the duct pulls only cobblestone from the west

## MODIFIED Requirements

### Requirement: Per side modes per channel
Each channel of a duct SHALL have one of the modes in and out, pull in, push out or disabled per side, default in and out, set through the duct GUI's mode button for the selected side and kept with the block. A disabled side SHALL not connect visually or logically.

#### Scenario: Set a mode
- **WHEN** the player sets the item channel's west side to pull in
- **THEN** the duct keeps that mode after reload and the GUI shows it
