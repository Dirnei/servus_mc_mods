# Spec Delta

## MODIFIED Requirements

### Requirement: Steps wait for warehouse power
A step that runs on a Crafting Station SHALL wait while that station cannot pay its work: the order stays on its CPU, keeps its reservations, buffered intermediates and progress, is not cancelled or replanned, and the Production Terminal SHALL show the step as waiting for power. Steps on other producers (Machine Adapters, Alchemy machines) SHALL run on their own fuel and power and SHALL NOT wait for the warehouse's power, but their inputs and outputs still move only as far as shuttles can run. The CPU itself SHALL draw no FE.

#### Scenario: Station without power
- **WHEN** an order for a crafting table runs its planks step on a Crafting Station and the warehouse's controller runs dry
- **THEN** the order stays, the Production Terminal shows the step waiting for power, and once FE arrives the step finishes and the order goes on

#### Scenario: Furnace keeps smelting
- **WHEN** an order smelts iron through a Machine Adapter on a vanilla furnace and the warehouse's controller runs dry after the ore arrived
- **THEN** the furnace keeps smelting the ore it holds
