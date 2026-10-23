## Purpose

Shuttle traffic says how shuttles share rails: they follow each other with a gap, pass when they drive in opposite directions, wait behind a shuttle that stands, merge in turn, never deadlock in a ring, leave their garage only onto a clear rail and are drawn where they really are.

## ADDED Requirements

### Requirement: Heading
A shuttle's heading SHALL be the direction of the next block on its way. A shuttle standing at the end of its way SHALL keep the heading of the last block it drove. Two shuttles SHALL share a lane at a rail when one of them drives into or out of that rail in a direction the other drives into or out of it too.

#### Scenario: Heading of a standing shuttle
- **WHEN** a shuttle drove east onto the rail of a bay and stands there for its stop
- **THEN** its heading is east

### Requirement: Shuttles with the same heading follow with a gap
A shuttle SHALL NOT come closer than the follow gap (`follow_gap` in `data/<ns>/shuttles/warehouse.json`, example 1 block from centre to centre) to a shuttle ahead of it in the same lane. It SHALL brake in time to keep that gap, at its normal braking rate, and drive no faster than the shuttle ahead while it is that close. Shuttles SHALL never be drawn inside each other while they follow.

#### Scenario: Fast shuttle behind a slow one
- **WHEN** a Mk3 shuttle with Speed upgrades drives behind a plain Mk1 shuttle along the same aisle
- **THEN** it catches up, brakes, and follows the Mk1 one block behind at the Mk1's speed until their ways part

#### Scenario: Two shuttles to the same rack
- **WHEN** two shuttles leave for two bays of the same rack section, one right after the other
- **THEN** the second drives behind the first and never overlaps it

### Requirement: Opposite headings pass
Shuttles whose ways cross a rail in opposite directions, or across each other without sharing a lane, SHALL NOT wait for each other.

#### Scenario: Passing in an aisle
- **WHEN** a shuttle drives east along an aisle while another drives west along the same rails
- **THEN** both keep their speed and pass through each other

### Requirement: A standing shuttle holds the shuttles behind it
A shuttle that stands on a rail, for its stop at a bay, station or gantry, for field work at a plot, for the lift car, for a deck or for power, SHALL hold every shuttle that would share its lane there: they stop the follow gap behind it and drive on once it has left. Shuttles that pass it with an opposite heading or across its lane SHALL NOT wait.

#### Scenario: Waiting behind a loading shuttle
- **WHEN** a shuttle stands at a bay for its stop and another shuttle comes along the same aisle in the same direction
- **THEN** the second stops one block behind it and drives on after the first has left

#### Scenario: Passing a loading shuttle the other way
- **WHEN** a shuttle stands at a bay for its stop, heading east, and another shuttle drives west along that aisle
- **THEN** the second drives past without stopping

### Requirement: Merging shuttles go in turn
When two shuttles come from different rails into the same lane, the one with the shorter way to the rail where they meet SHALL go first and the other SHALL follow it with the gap; on equal ways the shuttle with the lower number SHALL go first, and a shuttle without a number after numbered ones.

#### Scenario: Merge at a junction
- **WHEN** a shuttle comes from the north and another from the west into a junction, both turning east, and the one from the west is closer
- **THEN** the one from the west goes first and the other follows it one block behind

#### Scenario: Tie by number
- **WHEN** Shuttle #4 and Shuttle #2 are equally far from the rail where their lanes meet
- **THEN** Shuttle #2 goes first

### Requirement: A ring of followers moves together
When shuttles wait for each other in a closed ring, each one following the next, they SHALL move on together at the speed of the slowest of them, keeping their gaps, instead of standing forever. A ring SHALL stand only while one of its shuttles stands for another reason (a stop, the lift car, a deck or power).

#### Scenario: Loop full of shuttles
- **WHEN** four shuttles drive round a loop of eight rails, each one block behind the next
- **THEN** they keep driving round together and none stands still for good

### Requirement: Leaving the garage
A shuttle given a job SHALL wait inside its garage, not drawn on the rail, until it can leave: until no shuttle that could not stop in time comes along the lane it takes, and until no shuttle stands in that lane on the home rail. Two shuttles leaving one garage SHALL leave one after the other, the gap apart, the lower number first. Parked shuttles, idle ones and ones parked with cargo SHALL stand inside the garage and SHALL never hold a shuttle on the home rail.

#### Scenario: Two jobs at once
- **WHEN** a garage with two idle shuttles gets two jobs in the same tick
- **THEN** one shuttle leaves first and the other leaves once the first is a block away, and they are never drawn inside each other

#### Scenario: Shuttle passing the garage
- **WHEN** a shuttle drives past a garage along its home rail at full speed while a shuttle of that garage gets a job in the same direction
- **THEN** the garage's shuttle waits inside until the passing shuttle has gone by, then follows it

### Requirement: The lift car is never blocked by its own queue
A shuttle the lift car has set down at a floor SHALL drive off behind the shuttles ahead of it like any other shuttle, keeping the car while it waits; but when the shuttles it would wait for are, one behind the other, waiting for that same car, it SHALL drive off without waiting, so the car becomes free.

#### Scenario: Shuttle ahead at the exit
- **WHEN** the car sets a shuttle down on a floor where another shuttle stands at a bay right beside the shaft, heading away from it
- **THEN** the shuttle waits on the car until the other has left and then drives off behind it

### Requirement: Followers are drawn where they are
Every player SHALL see a shuttle that brakes for, stops behind or starts again after the shuttle ahead at the place where the server has it: the server SHALL sync a shuttle when it starts or stops following, and clients SHALL draw a following shuttle braking for the shuttle ahead between syncs. A standing shuttle SHALL be drawn on its rail, not moved aside.

#### Scenario: Braking behind a stopping shuttle
- **WHEN** a player watches a shuttle follow another that stops at a bay
- **THEN** the follower slows down and stands one block behind without running into it or jumping back

#### Scenario: Standing shuttles stay put
- **WHEN** one shuttle stands at a bay and a second shuttle stops at another bay
- **THEN** neither is drawn shifted to the side
