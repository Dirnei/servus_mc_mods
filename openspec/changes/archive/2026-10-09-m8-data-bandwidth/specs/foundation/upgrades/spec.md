# Spec Delta

## Purpose

Upgrades are items that make a Servus block faster when placed into its upgrade slots, defined in Foundation so every Servus mod can use them and other mods can add better ones.

## ADDED Requirements

### Requirement: Speed Upgrade item
Foundation SHALL provide a Speed Upgrade item crafted from vanilla materials (example: redstone, gold ingots and a copper ingot) with a speed multiplier from datapack JSON (example 2).

#### Scenario: Crafting
- **WHEN** the player crafts the Speed Upgrade recipe
- **THEN** one Speed Upgrade is produced

### Requirement: Upgrade slots
A block with upgrade slots SHALL accept only upgrade items in them, at most one item per slot, and SHALL multiply its rate by the product of the multipliers of the upgrades it holds. Upgrades SHALL drop when the block is broken.

#### Scenario: Two upgrades
- **WHEN** a block with a base rate of 8 holds two Speed Upgrades of multiplier 2
- **THEN** its rate is 32

#### Scenario: Wrong item
- **WHEN** the player tries to put cobblestone into an upgrade slot
- **THEN** the slot refuses it

### Requirement: Upgrades from other mods
Other mods SHALL be able to register their own upgrade items with their own multiplier through Foundation, and every block with upgrade slots SHALL accept them.

#### Scenario: Better upgrade
- **WHEN** a mod registers an upgrade item with multiplier 4 and the player puts it into a Packing Station
- **THEN** the station's rate is multiplied by 4
