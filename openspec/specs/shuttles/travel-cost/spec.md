# shuttles/travel-cost Specification

## Purpose
Travel cost tells the warehouse how long a shuttle needs for a way over its rails and lifts, so shuttles drive the quickest way and jobs go to the shuttles, bays and stations that are quickest to reach, not just the fewest blocks away.

## Requirements

### Requirement: Cost of a way is its travel time
The cost of a way over rails and lift shafts SHALL be its estimated travel time in ticks: a weight per rail block, an extra weight per corner, a weight per block ridden in a lift car, a handover weight per ride and a stop weight per stop at a block. The weights SHALL come from `travel_costs` in `data/<ns>/shuttles/warehouse.json` (example: rail block 7, corner 5, lift block 10, lift handover 20, stop 14, lift wait per job 50).

#### Scenario: Long flat way beats a lift
- **WHEN** a bay is reached either by 10 rail blocks on the same floor or by 3 rail blocks and a one floor lift ride whose car waits three floors away
- **THEN** the flat way is cheaper

### Requirement: Waiting for a lift costs time
Entering a ride SHALL add the wait for the car, read when the cost is asked for: the car's distance to the floor times the lift block weight, plus the lift wait per job for every call queued at the car and every shuttle already on a path through that shaft.

#### Scenario: Busy lift
- **WHEN** two equal ways lead to the same floor through two shafts and one shaft's car has three calls queued
- **THEN** the way through the other shaft is cheaper

### Requirement: Cost decides ways and choices
A shuttle SHALL drive the cheapest way to the goal of its leg. The free shuttle with the cheapest way to a job's first stop SHALL take the job. Put-away targets SHALL be compared by the cost from the station to the bay, pick sources by the cost from the bay to the destination, and Packing Stations by the cost from the racks holding the item. Exact ties SHALL be broken by the order in which the blocks joined the warehouse.

#### Scenario: Corner avoided
- **WHEN** two ways of equal length lead to a rack, one straight and one with four corners
- **THEN** the shuttle drives the straight way

#### Scenario: Shuttle by travel time
- **WHEN** shuttle A is 6 rail blocks from a station on the same floor and shuttle B is 3 blocks away but behind a lift ride
- **THEN** shuttle A takes the put-away
