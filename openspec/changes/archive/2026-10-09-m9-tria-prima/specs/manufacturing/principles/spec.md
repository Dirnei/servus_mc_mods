# Spec Delta

## Purpose

The three principles of Paracelsus' Tria Prima, Salt, Sulfur and Mercury, are the fluids every alchemical machine of Manufacturing works with: the Calciner makes them, the Coagulator and later blocks consume them.

## ADDED Requirements

### Requirement: Three principle fluids
Manufacturing SHALL register three fluids, Salt, Sulfur and Mercury (`servus_manufacturing:salt`, `servus_manufacturing:sulfur`, `servus_manufacturing:mercury`), each with its own fluid type, a source and a flowing form and a distinct colour.

#### Scenario: Shown in a tank
- **WHEN** a Fluid Tank holds 1,000 mB of Sulfur
- **THEN** the tank shows a yellow fluid and its read out names Sulfur

### Requirement: Principles move like any fluid
The principles SHALL be ordinary fluids for every fluid handler: the Fluid Pipe moves them, the Fluid Tank stores them, the Sink voids them, and other mods' fluid blocks can hold them.

#### Scenario: Pipe to a tank
- **WHEN** a Fluid Pipe connects a block offering Salt with a Fluid Tank
- **THEN** the Salt ends up in the tank

### Requirement: Buckets of principles
Each principle SHALL have a bucket. A bucket SHALL fill from and empty into fluid handlers like a water bucket, and SHALL place the principle as a liquid in the world, which flows like water without making infinite sources.

#### Scenario: Bucket from a tank
- **WHEN** a player uses an empty bucket on a Fluid Tank holding 1,000 mB of Mercury
- **THEN** the player holds a Mercury bucket and the tank is empty

#### Scenario: No infinite principle
- **WHEN** a player pours two Salt buckets into a 2 by 1 hole
- **THEN** the liquid does not spread into new source blocks
