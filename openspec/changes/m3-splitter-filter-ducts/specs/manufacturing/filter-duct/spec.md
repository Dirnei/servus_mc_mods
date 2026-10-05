# Spec Delta

## Purpose

The Filter Duct sorts: inventories next to it only receive the items on its whitelist.

## ADDED Requirements

### Requirement: Whitelist set in the GUI
A Filter Duct SHALL open a GUI with nine ghost slots. Clicking a slot with an item sets that item on the list without consuming it; clicking with an empty hand clears it. The list SHALL persist with the block.

#### Scenario: Set a filter
- **WHEN** the player clicks a ghost slot while holding iron ingots
- **THEN** the slot shows iron ingots and the player still holds them

### Requirement: Only listed items are delivered through the filter
The duct network SHALL deliver to an inventory adjacent to a Filter Duct only items that match an entry on that duct's list. With an empty list every item passes.

#### Scenario: Sorting line
- **WHEN** iron ingots and copper ingots travel a network with a filter duct listing iron ingots beside chest A and a plain duct beside chest B
- **THEN** chest A receives only iron ingots and chest B receives the rest

#### Scenario: Empty list
- **WHEN** a filter duct has no entries
- **THEN** its neighbours receive items like beside a plain duct

### Requirement: Obtained through research
Filter Ducts SHALL be assembled from item ducts and copper wire, gated by the splitter_filter_ducts tech.

#### Scenario: Gated
- **WHEN** a team without the tech adds the recipe to an assembler
- **THEN** it is refused with the research message
