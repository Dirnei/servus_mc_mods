## Purpose

The Principle Dropper automates feeding a Gnome Hollow in Citrinitas: fed by pipes, it drips the principles onto the Hollow below it, so no pipe ever touches the disguised Hollow.

## ADDED Requirements

### Requirement: Principle tanks fed from outside
The Principle Dropper SHALL hold one tank per principle (example 1,000 mB each). It SHALL expose the standard fluid capability on every side, which SHALL fill the tank of the offered principle and refuse any other fluid, and SHALL never give fluid back. A player SHALL pour a principle bucket into it by using the bucket on it, with or without Elemental Sight, and get the empty bucket back.

#### Scenario: Filled by capability
- **WHEN** 500 mB Salt and 500 mB Water are offered to a Dropper through its fluid capability
- **THEN** its Salt tank holds 500 mB and the Water is refused

#### Scenario: Not drained
- **WHEN** a pipe or a block tries to drain a Dropper holding Mercury through its fluid capability
- **THEN** nothing comes out

#### Scenario: Bucket by hand
- **WHEN** a player without Elemental Sight uses a Mercury bucket on a Dropper
- **THEN** its Mercury tank gains 1,000 mB and the player holds an empty bucket

### Requirement: Dripping onto the Hollow below
A Dropper standing directly on top of a Gnome Hollow SHALL, at the drip interval (example every 20 ticks), move up to the drip amount (example 25 mB) of each principle it holds into the Hollow's tank of that principle, as long as the Hollow's vein is seeded and not ripe, the vein's ripening step takes that principle, and the Hollow's tank has room. It SHALL move nothing into a Hollow without a vein, into a ripe vein, or any principle the vein does not take. The Hollow SHALL keep exposing no capability, and SHALL ripen from dripped principles exactly as from poured ones.

#### Scenario: Ripens without buckets
- **WHEN** a Dropper holding Salt and Mercury stands on an unripe iron Hollow with empty tanks
- **THEN** Salt and Mercury drip into the Hollow and its vein grows steps

#### Scenario: Only what the vein takes
- **WHEN** a Dropper holding Sulfur stands on an unripe iron Hollow whose steps take only Salt and Mercury
- **THEN** the Sulfur stays in the Dropper

#### Scenario: Ripe vein
- **WHEN** a Dropper holding Salt stands on a Hollow whose vein is ripe
- **THEN** the Salt stays in the Dropper

#### Scenario: Empty Hollow
- **WHEN** a Dropper holding Salt stands on a Hollow without a vein
- **THEN** the Salt stays in the Dropper

### Requirement: Drips only onto a Hollow
A Dropper SHALL move principles only into a Gnome Hollow directly below it. Above any other block, including a block that takes fluids, it SHALL move nothing and keep its tanks.

#### Scenario: Above stone
- **WHEN** a Dropper holding Salt stands on plain stone for several drip intervals
- **THEN** its Salt tank keeps its amount

#### Scenario: Above a Fluid Tank
- **WHEN** a Dropper holding Salt stands on an empty Fluid Tank for several drip intervals
- **THEN** the Fluid Tank stays empty and the Dropper keeps its Salt

### Requirement: Drip numbers are data
The drip interval, the amount per drip and the tank size SHALL come from the Dropper's machine stats in datapack JSON, falling back to code defaults.

#### Scenario: Faster drip by datapack
- **WHEN** a datapack sets the Dropper's drip amount to 100 mB
- **THEN** each drip moves up to 100 mB of each principle

### Requirement: Drips are seen
While a Dropper drips a principle into the Hollow below, clients near it SHALL see small drops in that principle's colour fall from its spout. A Dropper that moves nothing SHALL show no drops.

#### Scenario: Drops while dripping
- **WHEN** a Dropper drips Salt into an unripe Hollow
- **THEN** pale drops of Salt fall from its spout

### Requirement: The disguise holds
A Dropper SHALL reveal nothing about the block below it to a player without Elemental Sight: Jade SHALL show the Dropper with its own tanks only, and the Hollow below SHALL still show as Stone.

#### Scenario: Jade on the Dropper
- **WHEN** a player without Elemental Sight looks at a Dropper standing on a working Hollow with Jade
- **THEN** Jade shows the Principle Dropper and its tanks and nothing about a vein, a gnome or a Hollow

### Requirement: Dropper crafting
The Principle Dropper SHALL be made on a Citrinitas Athanor (tier 2 or higher) from a Principle Vat and principles from the tanks of its build (example 250 mB each of Salt, Sulfur and Mercury). An Athanor below Citrinitas SHALL not make it. Broken, it SHALL drop itself; the principles in its tanks are lost.

#### Scenario: Albedo Athanor
- **WHEN** a Principle Vat lies on a formed Albedo Athanor whose tanks hold every principle
- **THEN** no Principle Dropper is made

#### Scenario: Citrinitas Athanor
- **WHEN** a Principle Vat lies on a formed Citrinitas Athanor whose tanks hold 250 mB of each principle
- **THEN** a Principle Dropper is made and the principles are used

### Requirement: Workshop entry
The journal's Workshop SHALL have a Principle Dropper entry that opens with the Wandering years chapter, tells how to make it, where to place it and that it drips onto the Hollow below until the vein is ripe.

#### Scenario: Before Wandering years
- **WHEN** a team has not finished Schwaz
- **THEN** the Principle Dropper entry is locked in the Workshop
