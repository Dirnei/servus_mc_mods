# lib/network-manager Specification

## Purpose

Turns loaded node blocks into live network objects, one per connected component and network type, keeps them in step with the world, delivers their events once per server tick, and ticks them in a fixed order.

## Requirements

### Requirement: A network exists per connected component
For each registered network type, the manager SHALL keep exactly one network object per connected component of loaded nodes of that type in a level.

#### Scenario: First node of a type
- **WHEN** a node block of type T is loaded and no other node of T is connected to it
- **THEN** a new network of type T exists containing only that node

#### Scenario: Node joins an existing network
- **WHEN** a node block is loaded next to a node of the same type that connects to it
- **THEN** the node is added to that existing network and no new network is created

### Requirement: Networks merge and split with the graph
When a node links two or more networks, the manager SHALL keep the largest and let it absorb the others. When removing a node splits a component, the largest part SHALL keep its network and each other part SHALL get a new network.

#### Scenario: Bridging node
- **WHEN** a node is loaded that connects a three node network and a one node network
- **THEN** the three node network absorbs the other one, is told about the absorbed network before it is disposed, and only one network of that type remains

#### Scenario: Cutting node
- **WHEN** the middle node of a chain network is removed
- **THEN** the larger remaining part keeps the original network and the smaller part gets a new network

### Requirement: Nodes leave on removal and unload
A node SHALL leave its network when its block entity is removed from the world or when its chunk is unloaded, and SHALL rejoin when the chunk loads again.

#### Scenario: Block broken
- **WHEN** a node block is broken
- **THEN** its network no longer contains that position and the network is disposed when it was the last node

#### Scenario: Chunk unloaded
- **WHEN** the chunk holding a node is unloaded
- **THEN** the node leaves its network as if removed, without the network being told the block was destroyed

### Requirement: Events are delivered once per tick in posting order
Events posted to a network's mailbox from any thread SHALL be handled on the server thread during that level's next tick, in the order they were posted, before the network's tick work runs.

#### Scenario: Two events in one tick
- **WHEN** events A then B are posted to a network between two ticks
- **THEN** on the next tick the network handles A, then B, then performs its tick work

#### Scenario: Event posted during handling
- **WHEN** a network posts an event to itself while handling another
- **THEN** the new event is handled on the following tick, not the current one

### Requirement: Tick order is deterministic
On every level tick the manager SHALL tick network types in registration order and networks within a type in ascending id order.

#### Scenario: Two networks of one type
- **WHEN** networks with ids 3 and 7 of the same type exist
- **THEN** network 3 ticks before network 7 on every tick

### Requirement: Levels are isolated
Nodes in different levels SHALL never share a network, and unloading a level SHALL dispose all of its networks.

#### Scenario: Same position in two levels
- **WHEN** node blocks of the same type sit at the same coordinates in the overworld and the nether
- **THEN** they belong to two different networks

#### Scenario: Level unloads
- **WHEN** a server level is unloaded
- **THEN** every network of that level is disposed and the manager for it is dropped

### Requirement: Pure core testable without Minecraft
The mailbox, network base class and network set SHALL have no dependency on Minecraft or NeoForge classes.

#### Scenario: Unit tests
- **WHEN** the core tests run under `gradlew :lib:test`
- **THEN** merge, split, event order and tick order are verified without launching the game
