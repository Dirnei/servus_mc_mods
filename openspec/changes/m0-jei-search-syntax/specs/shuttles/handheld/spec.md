# Spec Delta

## MODIFIED Requirements

### Requirement: STOCK mode
The STOCK screen SHALL list the items stored in the bound warehouse with their counts, largest first, with a search line that filters with Foundation's search syntax (name, and the prefixes for mod, tag, tooltip, creative tab and id). Right clicking an item in the list SHALL close the GUI and colour every loaded shelf of the bound warehouse holding that item, a tint and an outline over all its bays, seen through blocks, for some seconds (example 8), while the handheld is in hand. The panel SHALL show the item, its total and each holding shelf's code once.

#### Scenario: Find from the stock list
- **WHEN** the bound warehouse holds bread in two bays of one shelf and in one bay of another shelf and the player right clicks bread on the STOCK screen
- **THEN** both shelves are tinted and outlined as a whole, seen through the walls, and the panel shows bread, its total and the two shelf codes

#### Scenario: Search the stock by tag
- **WHEN** the bound warehouse holds iron ore, coal ore and bread and the player types `$ores` in the STOCK search line
- **THEN** only iron ore and coal ore are listed

#### Scenario: Glow only in hand
- **WHEN** shelves glow after a find and the player switches to another hotbar slot
- **THEN** no shelf is tinted until the handheld is in hand again

#### Scenario: Glow ends
- **WHEN** the glow time has passed
- **THEN** no shelf is tinted or outlined any more
