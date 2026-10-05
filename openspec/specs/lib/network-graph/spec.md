# lib/network-graph Specification

## Purpose

Tracks which network nodes belong together. Given nodes at block positions and a rule for when two neighbours connect, the graph keeps connected components with stable ids and reports every merge and split so network objects can follow.

## Requirements

### Requirement: Nodes connect through a symmetric rule
Two nodes at adjacent positions SHALL be connected only when the connection rule accepts the pair from both sides. A rule that accepts A to B but rejects B to A yields no edge.

#### Scenario: Both sides accept
- **WHEN** nodes A and B are adjacent and the rule accepts A to B and B to A
- **THEN** B is a neighbour of A and A is a neighbour of B

#### Scenario: One side rejects
- **WHEN** nodes A and B are adjacent and the rule rejects B to A
- **THEN** neither node lists the other as a neighbour and they are in different components

### Requirement: Adding a node creates or joins a component
Adding a node with no connected neighbours SHALL create a new component. Adding a node connected to exactly one component SHALL join that component and keep its id.

#### Scenario: Isolated node
- **WHEN** a node is added with no connected neighbours
- **THEN** the change reports one created component containing only that node

#### Scenario: Extending a chain
- **WHEN** a node is added next to one existing component
- **THEN** the node belongs to that component, its id is unchanged, and the change reports no created or removed components

### Requirement: Adding a node merges components
Adding a node connected to two or more components SHALL merge them into one. The largest component keeps its id and the change SHALL report the absorbed ids.

#### Scenario: Bridging two chains
- **WHEN** a node is added between a component of three nodes and a component of one node
- **THEN** all five nodes share the id of the three node component and the change reports the one node component as merged away

### Requirement: Removing a node shrinks, splits or deletes its component
Removing a node SHALL keep the component when the remaining nodes are still connected, split it into one component per remaining connected part when they are not, and delete it when no node remains. On a split the largest part keeps the id and the other parts get new ids reported as created.

#### Scenario: Removing from a ring
- **WHEN** a node is removed from a four node ring
- **THEN** the three remaining nodes stay in the same component with the same id

#### Scenario: Cutting a chain
- **WHEN** the middle node of a five node chain is removed
- **THEN** the two node part and the two node part form two components, one keeps the old id and the other is reported as created

#### Scenario: Last node
- **WHEN** the only node of a component is removed
- **THEN** the change reports that component as removed and the id is no longer known to the graph

### Requirement: Component queries
The graph SHALL answer which component a position belongs to, which positions a component contains, which positions neighbour a position, and which component ids exist.

#### Scenario: Membership after a merge
- **WHEN** two components were merged by a bridging node
- **THEN** the component of any former member equals the survivor id and the survivor's member set contains all of them

#### Scenario: Unknown position
- **WHEN** a position that holds no node is queried
- **THEN** the graph reports no component for it and no neighbours

### Requirement: Pure Java
The graph package SHALL have no dependency on Minecraft or NeoForge classes so it can be unit tested with JUnit alone.

#### Scenario: Unit test without a game
- **WHEN** the graph tests run under `gradlew :lib:test`
- **THEN** they pass without launching Minecraft
