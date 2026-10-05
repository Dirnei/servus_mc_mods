# manufacturing/item-duct Specification

## Purpose

The Item Duct moves pushed items from machines to the next inventory that wants them, so a Tier 1 factory can be laid out without every block touching.

## Requirements

### Requirement: Ducts connect to ducts and inventories
A duct SHALL show an arm toward every neighbour that is a duct or exposes the item handler capability on the facing side, updating when neighbours change, and connected ducts SHALL form one network.

#### Scenario: Duct between machine and chest
- **WHEN** a duct is placed between a smelter and a chest
- **THEN** it shows arms toward both and belongs to a network containing itself

#### Scenario: Chain
- **WHEN** four ducts are placed in a row
- **THEN** they form one network of four members

### Requirement: Inserted items are routed to an accepting inventory
Any item inserted into a duct from a neighbour SHALL be delivered at once to another inventory on the duct's network that accepts it, never to the inserting neighbour and never into another duct. When no inventory accepts, the insertion SHALL be refused so the item stays where it was.

#### Scenario: Extractor to smelter through ducts
- **WHEN** an extractor pushes raw iron into a duct whose network also touches a smelter
- **THEN** the raw iron lands in the smelter's input slot

#### Scenario: Nothing accepts
- **WHEN** the only other inventory on the network is full
- **THEN** the machine keeps its output and the duct holds nothing

#### Scenario: Round robin
- **WHEN** two chests on the network both accept the pushed items
- **THEN** successive items alternate between the chests

### Requirement: Ducts hold nothing
A duct SHALL never store items and SHALL expose no extractable content, so breaking a duct loses nothing.

#### Scenario: Break duct
- **WHEN** a duct is broken
- **THEN** it drops itself and no items

### Requirement: Throughput is data
A duct network SHALL move at most `items_per_tick` items per tick, read from `data/<ns>/machine/item_duct.json` (default 16).

#### Scenario: Budget exhausted
- **WHEN** more items are offered in one tick than the budget allows
- **THEN** the excess is refused and offered again by the machines later

### Requirement: Crafting
Item Ducts SHALL be craftable in bulk from iron and glass.

#### Scenario: Recipe
- **WHEN** the player crafts with iron ingots and glass in the duct pattern
- **THEN** the result is eight Item Ducts
