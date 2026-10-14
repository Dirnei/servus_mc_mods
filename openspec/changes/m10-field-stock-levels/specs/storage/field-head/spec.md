# Spec Delta

## ADDED Requirements

### Requirement: Produce learned from harvests
A Field Head SHALL learn its field's produce from its harvests: after a run that harvested, the produce SHALL be the item the harvest brought home the most of, leaving out the field's crop item whenever another item came home too. Choosing another crop SHALL forget the produce until the next harvest. The produce SHALL be kept when the world is saved.

#### Scenario: Wheat
- **WHEN** a wheat field's harvest brings home 12 wheat and 20 wheat seeds
- **THEN** the head's produce is wheat

#### Scenario: Carrots
- **WHEN** a carrot field's harvest brings home only carrots
- **THEN** the head's produce is carrots

#### Scenario: New crop
- **WHEN** a head whose produce is wheat gets beetroot as its crop
- **THEN** the head has no produce until its first beetroot harvest

### Requirement: Stock levels per head
Each Field Head SHALL keep a harvest limit and a fertilize level, both counted in items of its produce in the warehouse of the served field, each 0 for none (defaults `harvest_limit` and `fertilize_below` in `field.json`, example 0). While both are set, the fertilize level SHALL never be above the harvest limit: moving one past the other SHALL move the other with it. While the head has no produce, both levels SHALL do nothing. The levels SHALL be kept when the world is saved.

#### Scenario: Defaults
- **WHEN** a player places a new Field Head
- **THEN** its harvest limit and fertilize level are 0 and the field harvests and fertilizes as before

#### Scenario: Levels stay in order
- **WHEN** the harvest limit is 512 and the player raises the fertilize level to 640
- **THEN** the harvest limit is 640 as well

### Requirement: Harvesting stops at the limit
While a head has a produce and a harvest limit, and the warehouse holds at least that many of the produce, the field SHALL harvest nothing: the warehouse SHALL queue no run for ripe plots, and a run that goes out to till or plant SHALL leave ripe plots standing. Once the stock drops below the limit, harvesting SHALL resume at the next field check. One run MAY bring the stock above the limit.

#### Scenario: Limit reached
- **WHEN** a wheat field with harvest limit 1,000 is fully ripe and the warehouse holds 1,024 wheat
- **THEN** no harvester leaves for that field and the wheat stays ripe on the field

#### Scenario: Stock used up
- **WHEN** an order takes 100 of those 1,024 wheat
- **THEN** a harvester leaves for the field at the next field check

### Requirement: Fertilizing only below the level
While a head has a produce and a fertilize level, the field SHALL call no Spreader while the warehouse holds at least that many of the produce, and no Spreader either while harvesting is stopped at the limit. Below the level, the head's fertilize mode, threshold and reserve SHALL apply as before.

#### Scenario: Enough wheat
- **WHEN** a wheat field fertilizes after planting with fertilize level 500, has newly planted plots, and the warehouse holds 640 wheat
- **THEN** no Spreader is called

#### Scenario: Wheat runs low
- **WHEN** the same warehouse drops to 400 wheat
- **THEN** the field calls a Spreader at the next field check

### Requirement: Stock levels in the GUI
The Field tab SHALL show the produce with its icon, or "learned at the first harvest", and the harvest limit with buttons for -64, -1, +1 and +64, where a shift click moves ten times as far, and "no limit" at 0. The Fertilizer tab SHALL show the fertilize level the same way. While harvesting is stopped at the limit, the Field tab's work status SHALL say so with the stock and the limit; while fertilizing waits on the level, the Fertilizer tab's status SHALL say so. Neither SHALL be a problem: no red marker and no red ripe share bar. The field command SHALL print the produce and both levels.

#### Scenario: Paused shown
- **WHEN** a player opens the GUI of a wheat field stopped at its limit of 1,000 with 1,024 wheat in stock
- **THEN** the Field tab says harvesting is paused with 1,024 of 1,000 wheat, and no tab has a red marker

#### Scenario: Shift steps
- **WHEN** the harvest limit is 0 and the player shift clicks +64
- **THEN** the harvest limit is 640
