# Spec Delta

## MODIFIED Requirements

### Requirement: Rules order when stock runs low
Every `stock_rule_interval` ticks (example 100, from `warehouse.json`) the warehouse SHALL check each rule: when the indexed count of the item plus the amount of its open work orders is below the minimum, it SHALL create a work order for the difference between the maximum and that sum. A rule whose item no Outbound Station's producer can make SHALL NOT create an order and SHALL show "no producer" as its state; a rule whose item is above the minimum SHALL show "ok"; a rule with an open order SHALL show "ordering".

#### Scenario: Low on sticks
- **WHEN** the rule keeps 8 to 16 sticks, the warehouse holds 2 and an Outbound Station faces a Crafting Station with the sticks recipe
- **THEN** within the interval an order for 14 sticks exists and the rule shows ordering

#### Scenario: Enough
- **WHEN** the rule keeps 8 to 16 sticks and the warehouse holds 10
- **THEN** no order is created and the rule shows ok

#### Scenario: Already ordering
- **WHEN** an order for 14 sticks is open and the count is still 2
- **THEN** no second order is created

#### Scenario: No producer
- **WHEN** a rule keeps 8 to 16 bread and nothing makes bread
- **THEN** no order is created and the rule shows no producer
