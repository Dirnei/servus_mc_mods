# foundation/energy-distribution Specification

## Purpose

One shared algorithm for moving energy from sources to sinks under a per endpoint rate, so cables and energy channels behave the same everywhere.

## Requirements

### Requirement: Energy moves from sources to sinks within the rate
Each distribution pass SHALL move at most `rate` energy out of any single source and into any single sink, and SHALL never move more than the sources can supply or the sinks can accept.

#### Scenario: One source, one sink
- **WHEN** a source holds 1000, a sink can accept 300 and the rate is 100
- **THEN** 100 moves, the source holds 900 and the sink gained 100

#### Scenario: Sink limited
- **WHEN** a source holds 1000, a sink can accept 30 and the rate is 100
- **THEN** 30 moves

### Requirement: Supply is shared fairly between sinks
When supply is short, sinks SHALL be served in round robin order with the same share per round, so no sink starves.

#### Scenario: Two sinks, short supply
- **WHEN** one source can give 100 this pass and two sinks each want 100
- **THEN** each sink receives 50

#### Scenario: One sink needs less
- **WHEN** supply is 100, sink A wants 20 and sink B wants 100
- **THEN** A receives 20 and B receives 80

### Requirement: Sources are drained in order
Energy SHALL be taken from the first source until it is exhausted for this pass before the next source is used, so a network with several generators empties them predictably.

#### Scenario: Two sources
- **WHEN** sources A and B each can give 100, sinks want 150 in total and the rate is 100
- **THEN** A gives 100 and B gives 50

### Requirement: Pure Java
The distributor SHALL depend only on small source and sink interfaces, not on Minecraft or NeoForge.

#### Scenario: Unit tests
- **WHEN** the distributor tests run under `gradlew :lib:test`
- **THEN** they verify the scenarios above without launching the game
