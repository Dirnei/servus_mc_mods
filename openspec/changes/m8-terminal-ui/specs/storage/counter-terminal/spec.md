# Spec Delta

## ADDED Requirements

### Requirement: Craftable entries
The stock grid SHALL also list every item a producer on the terminal's data line can make, from the patterns of the line and the recipes producers report. A craftable item that is not in stock SHALL show "Craft" instead of a count; a craftable item that is in stock SHALL show its count and a craftable marker. A filter button next to the search box SHALL cycle the grid between All, Stock and Craftable.

#### Scenario: Sticks craftable
- **WHEN** no sticks are in stock and a sticks pattern and a Crafting Station are on the terminal's data line
- **THEN** the grid shows sticks with "Craft"

#### Scenario: Filter
- **WHEN** the player sets the filter to Craftable
- **THEN** only craftable items are shown, stocked or not

### Requirement: Craft dialog
Clicking a craftable item that is not in stock, or Ctrl clicking or middle clicking a craftable item that is in stock, SHALL open a craft dialog with an amount field, +1, +10 and +stack buttons, Start and Cancel. While the dialog is open the terminal SHALL show a preview the server computes with the order planner for that amount: what is taken from stock, what is crafted and what is missing. Start SHALL be disabled, with the reason shown, while raw materials are missing or no Crafting CPU is on the line.

#### Scenario: Preview
- **WHEN** the player opens the craft dialog for 8 sticks and the warehouse holds 16 oak planks
- **THEN** the preview shows 4 oak planks from stock and 8 sticks to craft, and Start is enabled

#### Scenario: Missing material
- **WHEN** the warehouse holds no planks and no logs
- **THEN** the preview lists oak planks as missing and Start is disabled

#### Scenario: No CPU
- **WHEN** no formed Crafting CPU is on the line
- **THEN** Start is disabled and the dialog says there is no CPU

### Requirement: Craft request
Start SHALL create a work order for the full amount on a Crafting CPU of the line, independent of the stock of the item itself, linked to a new request of the terminal that shows producing. The crafted items SHALL be put away and delivered into the output slots like any request. Cancelling the request SHALL cancel the order.

#### Scenario: Craft sticks
- **WHEN** the player starts crafting 8 sticks
- **THEN** a request for 8 sticks is listed as producing, and 8 sticks land in the output slots once crafted

### Requirement: Screen layout
The Counter Terminal screen SHALL give the search box and filter, the stock grid with its scrollbar, the request list, the output slots and the player inventory each their own area that no other element overlaps, and the panel SHALL fit a 1080p screen at the automatic GUI scale.

#### Scenario: Nothing overlaps
- **WHEN** the screen is open with requests listed and a message shown
- **THEN** no label, list row or slot overlaps another area

## MODIFIED Requirements

### Requirement: Live stock while open
The terminal SHALL send its full content (stock, craftable items, requests, rules) as soon as the screen opens, without waiting for the periodic update, and the screen SHALL show a loading hint until it arrives. While the GUI is open the stock shown SHALL refresh within half a second after the index changes, without resending when nothing changed.

#### Scenario: Machine output arrives
- **WHEN** a hopper puts 10 bread into a crate while the GUI is open
- **THEN** the bread count rises within a second

#### Scenario: Instant content
- **WHEN** the player opens the terminal
- **THEN** the stock is sent in the same tick the screen opens
