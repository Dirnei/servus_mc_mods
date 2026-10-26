# Spec Delta

## ADDED Requirements

### Requirement: Material result preview
While the order grid holds a material pattern set to Any, its result SHALL show the results of the variants in turn, one per second, and its tooltip SHALL add which ingredient the material follows ("Wood follows the planks"). The amount panel SHALL show the one variant the order will make.

#### Scenario: Rack preview cycles
- **WHEN** the order grid holds a rack recipe with oak planks set to Any
- **THEN** the result shows racks of different woods in turn and its tooltip says the wood follows the planks

#### Scenario: Amount panel names the variant
- **WHEN** the warehouse holds 40 birch planks, 5 oak planks and iron, and the player presses Next on that grid
- **THEN** the amount panel's To craft lists birch Heavy Duty Racks and From stock lists birch planks

## MODIFIED Requirements

### Requirement: Craftable entries
The stock grid SHALL also list every item a producer on the terminal's data line can make, from the patterns of the line and the recipes producers report. A material pattern set to Any SHALL be listed once, as its item without its material, with a tooltip line saying which ingredient the material follows, instead of one entry per variant. A craftable item with nothing requestable SHALL show "Craft" instead of a count; a craftable item that is requestable SHALL show its count and a craftable marker. A view button in the side column SHALL cycle the grid between All, Stock and Craftable.

#### Scenario: Sticks craftable
- **WHEN** no sticks are in stock and a sticks pattern and a Crafting Station are on the terminal's data line
- **THEN** the grid shows sticks with "Craft"

#### Scenario: Filter
- **WHEN** the player sets the view to Craftable
- **THEN** only craftable items are shown, stocked or not

#### Scenario: One rack entry
- **WHEN** the line has an Any rack pattern saved with oak planks and a Crafting Station
- **THEN** the grid shows one Heavy Duty Rack craft entry without a wood, its tooltip says the wood follows the planks, and no entry per wood

#### Scenario: Exact rack listed by its wood
- **WHEN** the line has only an Exact rack pattern with spruce planks
- **THEN** the grid shows a spruce Heavy Duty Rack craft entry

### Requirement: Exact or Any in the order grid
The order grid SHALL have the Exact / Any button of the recipe editor, with the same labels, greying and tooltips. It SHALL start as Any when Any is possible for the grid, also for a material grid such as a rack, and as Exact otherwise. Next and Back SHALL keep the chosen mode; Cancel, Escape and a started order SHALL reset it, so the grid opened empty starts as Any again.

#### Scenario: Sticks start as Any
- **WHEN** the player opens the order grid and puts a plank above a plank
- **THEN** the button shows Any, and pressing it switches to Exact

#### Scenario: Rack starts as Any
- **WHEN** the player presses JEI's transfer button on the Heavy Duty Rack recipe
- **THEN** the order grid opens with the rack recipe and the button shows Any

#### Scenario: Iron block stays Exact
- **WHEN** the order grid holds an iron block recipe
- **THEN** the button shows Exact, is greyed out and its tooltip says the cells take no other items

#### Scenario: Next and Back keep the mode
- **WHEN** the grid holds sticks set to Exact and the player presses Next and then Back
- **THEN** the button still shows Exact
