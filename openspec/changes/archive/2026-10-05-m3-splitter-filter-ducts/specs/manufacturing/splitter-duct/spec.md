# Spec Delta

## Purpose

The Splitter Duct feeds a duct network from inventories: it pulls from its neighbours and the network splits the items over everything that accepts them.

## ADDED Requirements

### Requirement: Splitter pulls from adjacent inventories
A Splitter Duct SHALL periodically take items from inventories adjacent to it, from slots those inventories allow extraction from, up to the configured amount per pull, and route them through its duct network to accepting inventories, never back to the source. When nothing on the network accepts an item, it SHALL stay in the source.

#### Scenario: Chest to machines
- **WHEN** a chest with raw iron touches a splitter duct and a smelter touches the same network
- **THEN** raw iron moves from the chest into the smelter's input over time

#### Scenario: Two targets
- **WHEN** two chests accept the pulled items
- **THEN** successive pulls alternate between them

#### Scenario: Nothing accepts
- **WHEN** no inventory on the network accepts the chest's items
- **THEN** the chest keeps them

### Requirement: Splitter is a duct
A Splitter Duct SHALL connect to ducts and inventories like a plain duct and join the same network.

#### Scenario: In a chain
- **WHEN** a splitter duct is placed between two plain ducts
- **THEN** all three form one network

### Requirement: Obtained through research
Splitter Ducts SHALL be assembled from item ducts and an iron gear, gated by the splitter_filter_ducts tech.

#### Scenario: Gated
- **WHEN** a team without the tech adds the recipe to an assembler
- **THEN** it is refused with the research message
