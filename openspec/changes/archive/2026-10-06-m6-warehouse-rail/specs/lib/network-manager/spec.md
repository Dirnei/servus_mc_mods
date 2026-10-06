# Spec Delta

## MODIFIED Requirements

### Requirement: Networks merge and split with the graph
When a node links two or more networks, the manager SHALL keep the largest and let it absorb the others. When removing a node splits a component, the largest part SHALL keep its network and each other part SHALL get a new network, and every node of such a part SHALL be told it left the old network and joined the new one.

#### Scenario: Bridging node
- **WHEN** a node is loaded that connects a three node network and a one node network
- **THEN** the three node network absorbs the other one, is told about the absorbed network before it is disposed, and only one network of that type remains

#### Scenario: Cutting node
- **WHEN** the middle node of a chain network is removed
- **THEN** the larger remaining part keeps the original network and the smaller part gets a new network

#### Scenario: Split handover
- **WHEN** the middle node of a six node chain is removed
- **THEN** the two nodes of the smaller part each leave the original network and join the new one, after the new network was created, and no node joins the original network
