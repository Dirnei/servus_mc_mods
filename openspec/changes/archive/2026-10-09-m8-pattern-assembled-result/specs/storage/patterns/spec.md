# Spec Delta

## MODIFIED Requirements

### Requirement: Patterns are data
A pattern SHALL be data kept by a Pattern Storage, never an item: one vanilla crafting recipe with its recipe id, a 3x3 template with one concrete item per used cell, and its result. No item SHALL represent a blank or an encoded pattern.

#### Scenario: No pattern items
- **WHEN** a player searches the creative inventory for patterns
- **THEN** there is no Blank Pattern and no Pattern item, only the Pattern Storage block

## ADDED Requirements

### Requirement: Pattern result is what the recipe makes
A pattern's result SHALL be the stack its recipe produces when crafted from the pattern's own template, including the components the recipe adds (for example the wood of the planks). Patterns kept from before SHALL get this result the first time their Pattern Storage is used on a loaded level, without the player saving them again.

#### Scenario: Rack from oak planks
- **WHEN** a pattern for the Heavy Duty Rack is saved with oak planks in its template
- **THEN** its result is an oak Heavy Duty Rack, the same item a Crafting Station crafts from it

#### Scenario: Requested racks arrive
- **WHEN** a player requests 2 Heavy Duty Racks that a CPU crafts from that pattern
- **THEN** the station's racks are collected and both arrive in the Counter Terminal

#### Scenario: Old pattern refreshed
- **WHEN** a Pattern Storage holding a rack pattern with a result without wood is loaded and its patterns are read
- **THEN** the pattern's result is the oak Heavy Duty Rack
