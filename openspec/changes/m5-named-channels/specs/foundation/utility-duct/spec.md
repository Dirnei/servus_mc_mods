# Spec Delta

## ADDED Requirements

### Requirement: Named channel per line per side
Each side of the item, power and fluid line SHALL be on one channel from the channel list, Default unless set, chosen in the endpoint GUI and kept with the block. Channels of the item, power and fluid lines on the same side are independent. Items, energy and fluid SHALL only move between sides on the same channel.

#### Scenario: Select a channel
- **WHEN** the player opens the endpoint GUI of the east side and picks "Iron ore" for the item line
- **THEN** the item line's east side stays on "Iron ore" after reload and the power and fluid lines' east sides stay on Default

#### Scenario: Routing by name
- **WHEN** chest A's pull side and chest B's push side are on "Iron ore" and chest C's push side is on Default
- **THEN** items from chest A reach chest B and never chest C

### Requirement: Channel popup
The channel button of a line page SHALL open a popup over the GUI with a search field, a toggle between "This cable" and "All", the matching channels with colour, icon and name, and "+ New". "This cable" SHALL list Default and the channels any loaded side of this line's connected run uses; "All" SHALL list Default and the player's team's channels. Picking an entry SHALL put the side on that channel. "+ New" SHALL open a form for name, colour and icon and, on save, create the channel and put the side on it.

#### Scenario: This cable
- **WHEN** the run of the item line uses "Iron ore" and "Gold" and the team also has "Main grid" used only on power lines
- **THEN** "This cable" lists Default, "Gold" and "Iron ore", and "All" lists "Main grid" too

#### Scenario: New channel for this side
- **WHEN** the player clicks "+ New", types "Copper", picks orange and saves
- **THEN** the side is on "Copper", its ring turns orange, and "Copper" is listed under "This cable"

#### Scenario: Search
- **WHEN** the popup is on "All" and the player types "ore"
- **THEN** only channels whose name contains "ore" are listed

## MODIFIED Requirements

### Requirement: Duct GUI
Using the Configuration Carrot, not sneaking, on an arm of a duct that touches an inventory or machine SHALL open the endpoint GUI for that side. It SHALL show the side and the touched block's name, one tab per present line on its left edge showing the line's item, and the page of the selected tab: the line's mode button, a channel button showing the side's channel with its colour, icon and name, and a remove button; the Item Pipe page also the filter slots and the priority with minus and plus buttons; the data line page only an on and off button and the remove button. The player inventory SHALL be shown under every page. Filter slots SHALL be hidden and inert on every page but the Item Pipe page. Clicking a filter slot with an empty cursor SHALL open a search popup over the GUI that lists every item with its icon and name, filtered by the typed text, with a "none" entry first; picking an entry SHALL set the slot, "none" SHALL clear it. Using the carrot on the centre or on an arm toward another duct SHALL open nothing. Bare hand right click SHALL open nothing.

#### Scenario: Configure one side
- **WHEN** the player uses the carrot on the west arm that touches a chest, sets the item mode to pull in and places cobblestone in a filter slot
- **THEN** the duct pulls only cobblestone from the west

#### Scenario: Two lines, two tabs
- **WHEN** the player opens the GUI of a duct with an Item Pipe and a Power Cable
- **THEN** two tabs show, the Item Pipe page is selected, and clicking the Power Cable tab shows its mode, channel button and remove button without filter slots

#### Scenario: Fluid Pipe tab
- **WHEN** the player opens the GUI of an arm whose duct carries all three lines and that touches a tank
- **THEN** three tabs show, and the Fluid Pipe page shows its mode, channel button and remove button without filter slots or priority

#### Scenario: Data tab
- **WHEN** the player opens the GUI of an arm toward a Warehouse Controller on a duct with the data line
- **THEN** the data page shows an on and off button and the remove button, without channel button, filter slots or priority

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

#### Scenario: Channel button
- **WHEN** the item line's side is on "Iron ore", red, with an iron ore icon
- **THEN** the channel button shows a red swatch, the iron ore icon and "Iron ore"

## REMOVED Requirements

### Requirement: Eight channels per line per side
**Reason**: Replaced by named channels from the team's channel list (`foundation/duct-channels`, "Named channel per line per side").
**Migration**: None, the mod is unreleased; sides saved on channels 2 to 8 load on Default.
