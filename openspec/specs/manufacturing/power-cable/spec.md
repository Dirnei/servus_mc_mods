# manufacturing/power-cable Specification

## Purpose

The Power Cable carries FE between generators and machines that are not adjacent. Connected cables form one network that balances energy every tick.

## Requirements

### Requirement: Cables connect visually and logically
A cable SHALL show an arm toward every neighbour that is a cable or exposes the energy capability on the facing side, and SHALL update when neighbours are placed or removed.

#### Scenario: Cable next to a generator
- **WHEN** a cable is placed beside a generator
- **THEN** the cable shows an arm toward the generator

#### Scenario: Neighbour removed
- **WHEN** the generator beside a cable is broken
- **THEN** the arm toward that side disappears

### Requirement: Connected cables form one network
All cables connected through their sides SHALL belong to one network; placing a cable between two networks merges them and removing one may split them.

#### Scenario: Chain
- **WHEN** five cables are placed in a row
- **THEN** they form one network with five members

#### Scenario: Cut
- **WHEN** the middle cable of the row is removed
- **THEN** two networks of two members each remain

### Requirement: Networks move energy every tick
Every server tick a cable network SHALL take energy from adjacent blocks that can provide it and give it to adjacent blocks that can accept it, limited by the cable transfer rate per endpoint, using the shared distribution rules.

#### Scenario: Generator to distant machine
- **WHEN** a running generator and a powered machine are joined by three cables
- **THEN** the machine's buffer fills at up to the cable rate per tick

#### Scenario: Two machines share
- **WHEN** one generator feeds two machines through one network and cannot cover both
- **THEN** both machines receive energy in equal shares

### Requirement: Cables carry no buffer
A cable SHALL not store energy and SHALL not expose the energy capability itself, so breaking a cable loses nothing.

#### Scenario: Break cable
- **WHEN** a cable is broken
- **THEN** it drops itself and no energy is lost from generators or machines

### Requirement: Crafting
Power Cables SHALL be craftable in bulk from copper and iron.

#### Scenario: Recipe
- **WHEN** the player crafts with copper ingots and iron ingots in the cable pattern
- **THEN** the result is several Power Cables (for example 8)
