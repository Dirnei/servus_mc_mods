# Spec Delta

## Purpose

The Machine Dock is where the warehouse meets a machine: it feeds ingredients into the block it faces and collects what that block pushes out, so shuttles can serve machines like any other stop on the rails.

## ADDED Requirements

### Requirement: Dock block and buffers
The Machine Dock SHALL be a block with a horizontal facing that points at the block it serves. It SHALL have a feed buffer and a collect buffer of `inbound_buffer_slots` slots each (example 9). Its item handler on every side SHALL accept inserts into the collect buffer and refuse extraction, so the machine it faces, hoppers and ducts can hand it items and nothing drains it. Breaking the dock SHALL drop both buffers.

#### Scenario: Machine pushes its output
- **WHEN** an Assembler faces a dock and finishes an iron gear
- **THEN** the gear lands in the dock's collect buffer within the machine's push interval

#### Scenario: Broken with contents
- **WHEN** the dock is broken while its buffers hold 10 bread
- **THEN** 10 bread drop at its position

### Requirement: Dock is a warehouse node without stock
The dock SHALL be a member of the warehouse network like an Inbound Station: it SHALL link to touching storage blocks, stations, garages and docks, rails SHALL dock onto it on every side but its front, and it SHALL NOT link to elevator segments. Its buffers SHALL NOT be indexed and the machine it faces SHALL NOT become a warehouse source through it. It SHALL show the connection lamp and the activity lamp like an Inbound Station.

#### Scenario: Machine is not stock
- **WHEN** an Assembler holds 20 iron ingots and faces a dock of an online warehouse
- **THEN** the terminal does not list the ingots

#### Scenario: Rail at the front
- **WHEN** a rail is placed where the dock's front points
- **THEN** the rail shows no dock piece towards it

### Requirement: Collected items are put away
Stacks in the collect buffer SHALL be put away into the warehouse exactly like an Inbound Station's buffer: by the zone routing, carried by a shuttle when one can reach the dock and the target, instantly otherwise, never instantly into a rack, and waiting with the red lamp and reason when there is no room.

#### Scenario: Output flows to storage
- **WHEN** an iron gear lands in the collect buffer and an unzoned crate has room
- **THEN** within a few ticks the gear is in the crate

### Requirement: Feed buffer is pushed into the machine
Every `dock_feed_interval` ticks (example 8) the dock SHALL push the stacks of its feed buffer into the item handler of the block it faces, through the side that faces the dock. Stacks the block refuses SHALL wait in the feed buffer. A dock facing a block without an item handler SHALL keep its feed buffer untouched.

#### Scenario: Feeding an assembler
- **WHEN** 4 iron ingots sit in the feed buffer of a dock facing an Assembler with the iron gear recipe configured
- **THEN** the ingots are in the Assembler's inputs within the feed interval

#### Scenario: Refused ingredient
- **WHEN** cobblestone sits in the feed buffer and the Assembler's recipes do not use it
- **THEN** the cobblestone stays in the feed buffer

### Requirement: Dock reports its producer
A dock facing a block with the Lib production capability SHALL report that block's recipes and active recipe to the warehouse. `/servus_warehouse producers <pos>` SHALL list every dock of the network at the position with the block it faces and its recipes, or say that a dock faces no producer.

#### Scenario: Assembler behind the dock
- **WHEN** an op runs the producers command on a warehouse whose dock faces an Assembler with the iron gear recipe
- **THEN** chat lists the dock, the Assembler and "iron gear from 2 iron ingot, 20 ticks"

#### Scenario: Chest behind the dock
- **WHEN** a dock faces a chest
- **THEN** the producers list says the dock faces no producer

### Requirement: Warehouse feeds a dock
The warehouse SHALL deliver a requested amount of an item into a dock's feed buffer when asked, planned like a terminal request: instantly from crates and foreign inventories no shuttle reaches, by shuttle from stock a shuttle can reach, never instantly from a rack. What the feed buffer cannot take SHALL wait like a terminal delivery. `/servus_warehouse feed <dock> <item> <count>` SHALL ask for that delivery and SHALL answer with the refusal reason when it cannot be made.

#### Scenario: Feed from a crate
- **WHEN** an op feeds 4 iron ingots to a dock while a crate touching the controller holds 10
- **THEN** 4 ingots are in the dock's feed buffer at once

#### Scenario: Feed from a rack
- **WHEN** an op feeds 4 iron ingots to a dock on the rail while the ingots sit in a rack and a garage has a shuttle
- **THEN** a shuttle brings the ingots to the dock

#### Scenario: Nothing to feed
- **WHEN** an op feeds an item the warehouse does not have
- **THEN** the command answers that nothing is in stock

### Requirement: Dock GUI
Right clicking the dock SHALL open a GUI with the feed row, the collect row, the player inventory and a state line that shows the backed up reason while the lamp is red or the offline reason. Shift clicking an inventory stack SHALL move it into the feed buffer.

#### Scenario: Hand feed
- **WHEN** the player shift clicks 8 iron ingots in the inventory
- **THEN** the ingots are in the feed buffer and are pushed into the machine

### Requirement: Dock is obtainable and dismantled
The dock SHALL be craftable from vanilla materials and a rail, SHALL drop itself when broken, and SHALL be dismantled by sneaking with the Configuration Carrot, dropping its buffers. The carrot without sneaking SHALL turn it.

#### Scenario: Crafting
- **WHEN** the player places iron ingots, a hopper, a chest and a rail in the dock pattern (for example hopper and chest over a rail, framed by ingots)
- **THEN** a Machine Dock is produced
