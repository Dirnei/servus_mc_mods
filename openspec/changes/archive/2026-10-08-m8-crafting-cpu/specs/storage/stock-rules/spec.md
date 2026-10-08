# Spec Delta

## MODIFIED Requirements

### Requirement: Rules order when stock runs low
Every `stock_rule_interval` ticks (example 100, from `warehouse.json`) the warehouse SHALL check each rule: when the indexed count of the item plus the amount of its open work orders on the CPUs of the controller's data line is below the minimum, a CPU of that line SHALL get a work order for the difference between the maximum and that sum. A rule whose controller reaches no CPU (neither the controller nor an Access Point of the warehouse on a data line with a CPU) SHALL show "no CPU". A rule whose item no producer of the line can make SHALL NOT create an order and SHALL show "no producer" as its state; a rule whose item is above the minimum SHALL show "ok"; a rule with an open order SHALL show "ordering".

#### Scenario: Low on sticks
- **WHEN** the rule keeps 8 to 16 sticks, the warehouse holds 2, and a Crafting Station with the sticks recipe and a CPU are on the warehouse's data line
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

#### Scenario: No CPU
- **WHEN** the rule keeps 8 to 16 sticks, the warehouse holds 2 and no CPU is on its data line
- **THEN** no order is created and the rule shows no CPU
