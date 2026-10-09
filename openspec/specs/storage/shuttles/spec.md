# storage/shuttles Specification

## Purpose

Shuttles are the warehouse's pickers: small carts that live on the controller, travel the rails to fetch requested items and are drawn moving so the warehouse looks alive.

## Requirements

### Requirement: Shuttle item and fleet
A Shuttle SHALL be a craftable item. Shuttles SHALL be kept in Shuttle Garages, each up to `garage_capacity` (in `data/<ns>/storage/warehouse.json`, example 4); the fleet of a warehouse is every shuttle of every garage in its network. The Warehouse Controller SHALL NOT take shuttles.

#### Scenario: Add and take back
- **WHEN** a player right clicks a garage twice with shuttles and then sneak right clicks it with an empty hand
- **THEN** the warehouse has one shuttle and the player got one shuttle item back

#### Scenario: Cap
- **WHEN** a garage already holds its capacity of shuttles
- **THEN** another shuttle item is refused and stays in hand

#### Scenario: Controller refuses shuttles
- **WHEN** a player right clicks the Warehouse Controller with a Shuttle item
- **THEN** the shuttle stays in hand and the controller shows its summary

### Requirement: Shuttles move on the rail graph
Each shuttle SHALL park on the home rail of its garage, the rail touching that garage. It SHALL travel only along rails and elevator segments at a speed from `warehouse.json` (`shuttle_speed`, example 0.25 blocks per tick). It SHALL change level only by riding a shaft's lift car. Stock in crates and foreign inventories that no shuttle can reach SHALL be served instantly. Stock in Heavy Duty Racks SHALL be served only by shuttles.

#### Scenario: Round trip
- **WHEN** a shuttle is sent to a crate eight rails away at a quarter block per tick
- **THEN** it reaches the rail beside the crate after about 32 ticks and is back at its garage after about 64

#### Scenario: No home rail
- **WHEN** a garage holds shuttles but no rail touches it
- **THEN** its shuttles stay parked inside, serve nothing, and the summary says they are parked without a rail

#### Scenario: No garage
- **WHEN** a warehouse has crates on a rail and no garage
- **THEN** requests for the crates' items are served instantly

#### Scenario: Rack without a shuttle
- **WHEN** a warehouse has a rack on a rail and no garage
- **THEN** a request for an item that only sits in the rack is refused with the reason that it needs a shuttle

### Requirement: Pick jobs
A request at the storage terminal, or a shipment to a data member, SHALL be split into pick jobs of up to one stack each, one per source block and stack. A job SHALL ride a shuttle when the source touches a rail or segment some garage's home rail reaches and the warehouse has a shuttle; a job for a crate or foreign inventory no shuttle can reach SHALL be served instantly; a job for a rack SHALL only ever ride a shuttle. The free shuttle with the shortest way SHALL take a job, drive to the source rail, take the stack, drive to the rail of the Access Point that ships the request and deliver. Pick jobs and put-away jobs SHALL share one queue and SHALL be taken in the order they were made.

#### Scenario: Pick by shuttle
- **WHEN** a player requests a stack of cobblestone and the only crate holding it stands beside a rail five blocks from a garage
- **THEN** nothing arrives at once, a shuttle drives there and to the Access Point, and the cobblestone travels on as packets into the terminal's output slots

#### Scenario: Request larger than a stack
- **WHEN** a player requests 100 cobblestone held in one crate
- **THEN** two pick jobs are made, of 64 and 36

#### Scenario: Queued jobs
- **WHEN** three requests are made with one shuttle
- **THEN** the shuttle serves them one after another in the order they were made

#### Scenario: Stock gone
- **WHEN** the crate is emptied by hand before the shuttle arrives
- **THEN** the shuttle returns with nothing and the request shows what is missing

#### Scenario: Mixed queue
- **WHEN** a put-away arrives at a station, then a request is made, with one shuttle that is busy
- **THEN** the shuttle does the put-away first and the pick after it

### Requirement: Delivery
A shuttle SHALL deliver its cargo into the delivery buffer of the block the job names: the outgoing buffer of an Access Point for a request or shipment, the buffer of an Outbound Station for a feed. It drives to the rail touching that block. When the buffer has no room the shuttle SHALL wait beside it with its cargo until there is room. When the block is gone the cargo SHALL drop where it was. A destination touching no rail SHALL NOT take shuttle deliveries, and requests for it SHALL be served instantly or refused.

#### Scenario: Player left
- **WHEN** the requesting player logs out before the shuttle arrives
- **THEN** the stack still reaches the terminal's output slots as packets and waits there

#### Scenario: Output full
- **WHEN** the terminal's output slots are full when the packets arrive
- **THEN** the packets wait at the terminal and land as soon as the player takes something out

#### Scenario: Terminal broken
- **WHEN** the terminal is broken while packets of its request are on the line
- **THEN** no item is lost: the packets settle as the data line rules say

#### Scenario: Terminal off the rails
- **WHEN** the only Access Point on the terminal's data line touches no rail and the player requests an item that only sits in a rack
- **THEN** the request is refused with the reason that there is no rail

#### Scenario: Feed to a dock
- **WHEN** a feed of 4 ingots is made for an Outbound Station on the rail
- **THEN** the shuttle unloads the ingots into the station's buffer

### Requirement: Shuttles are drawn moving
Every player who can see a garage SHALL see its shuttles that are out on a job as small carts on the rails, moving smoothly along their paths with their cargo shown on top. Idle shuttles SHALL sit inside their garage and SHALL NOT be drawn on the rail; a cart appears on the garage's home rail when it leaves and disappears into the garage when it is back. A shuttle SHALL show its cargo only on the legs where it carries something.

#### Scenario: Watching a pick
- **WHEN** a player stands in the aisle while a pick runs
- **THEN** a cart drives past along the rail, with the picked item visible on its way to the terminal

#### Scenario: Idle fleet
- **WHEN** a garage holds three idle shuttles
- **THEN** no cart is drawn on its rail and the garage's right click says three shuttles, none out

#### Scenario: Watching a put-away
- **WHEN** a shuttle carries cobblestone from the station to a crate
- **THEN** the cart shows cobblestone on that leg and nothing on the way home

### Requirement: Shuttles ride the lift car between floors
A shuttle whose way leads up or down a shaft SHALL stop on the rail beside the shaft and call the car. It SHALL wait there until the car stands at its floor and its call is first in line. Then it drives onto the car, rides to the floor where its way leaves the shaft, and drives off. A shuttle that waits SHALL be drawn standing still.

#### Scenario: Pick from the upper floor
- **WHEN** a garage is on the ground floor, the car is on the ground floor, and a request needs a rack two floors up
- **THEN** the shuttle drives into the car, rides up, drives to the rack, comes back into the car, rides down and delivers

#### Scenario: Waiting for the car
- **WHEN** a shuttle reaches a shaft whose car is two floors up with another shuttle on board
- **THEN** the shuttle stands still on the rail beside the shaft until the car has delivered the other shuttle and come down for it

#### Scenario: Goal inside the shaft
- **WHEN** a shuttle's goal is a rail level of the shaft itself
- **THEN** it ends its leg on the car at that floor and keeps the car until it drives out

### Requirement: Shuttles cross a shaft on its deck
A shuttle that passes through a segment on one floor without changing level SHALL NOT call the car. It SHALL wait on the rail beside the shaft while the deck there is folded for a moving car, and SHALL cross as soon as the deck is down or the car stands at that floor. The car SHALL NOT leave or pass a floor while a shuttle is crossing it.

#### Scenario: Deck down
- **WHEN** a shuttle's way crosses a shaft from west to east on the ground floor and the car is parked two floors up
- **THEN** the shuttle drives straight through without stopping

#### Scenario: Car in the way
- **WHEN** the car is moving through that floor as the shuttle arrives
- **THEN** the shuttle waits beside the shaft until the car has passed and the deck is down, then crosses

#### Scenario: Car parked at the floor
- **WHEN** the car stands at that floor with no shuttle on board
- **THEN** the shuttle crosses over the car's platform

### Requirement: A broken shaft drops its rider's cargo
When a segment of a shaft is removed, every shuttle riding that shaft's car or waiting for it SHALL lose its cargo the way it would with its garage gone, and SHALL be sent home. If it has no way home, it SHALL be returned to its garage. A job not yet loaded SHALL go back to the queue. Shuttles crossing the shaft on a deck SHALL carry on.

#### Scenario: Segment broken during a ride
- **WHEN** a player breaks a middle segment while the car carries a shuttle with bread
- **THEN** the bread drops at the shuttle's garage, the request shows the bread as missing, and the shuttle returns to its garage

#### Scenario: Empty ride
- **WHEN** the shaft breaks while the car carries an empty shuttle on its way to a source
- **THEN** the pick job goes back to the front of the queue and nothing drops
