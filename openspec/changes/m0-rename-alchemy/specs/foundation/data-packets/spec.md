# Spec Delta

## MODIFIED Requirements

### Requirement: Delivery into a member
Items SHALL be inserted on the face of the target that touches the data line: into its packet inlet when it has one there, else into its item handler (foreign machines, the Machine Adapter, Alchemy machines), else into its stock sink. A move SHALL take items from its source only as far as the target accepts them, so items that do not fit stay at the source; a delivery of a stack the caller handed over SHALL keep what does not fit and offer it again every `retry_interval` ticks (example 20). The delivery SHALL count only items that were accepted.

#### Scenario: Station accepts planks
- **WHEN** a move of 8 planks reaches a Crafting Station with a job that uses planks
- **THEN** the planks are in the station's inputs

#### Scenario: Target full
- **WHEN** a move of 8 planks goes from a Crafting Station's outputs to a target with room for 3
- **THEN** 3 are inserted, 5 stay in the Crafting Station, and they follow once room frees up

#### Scenario: Foreign machine through the adapter
- **WHEN** a move of 8 cobblestone reaches a Machine Adapter on a vanilla furnace
- **THEN** the cobblestone goes into the furnace through the adapter's item handler
