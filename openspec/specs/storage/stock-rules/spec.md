# storage/stock-rules Specification

## Purpose

Stock rules keep a warehouse stocked on its own: an item with a minimum and a maximum is reordered whenever it runs low.

## Requirements

### Requirement: Rules are kept on the warehouse
A stock rule SHALL name one item, a minimum and a maximum with minimum below maximum. Rules SHALL be stored on the Warehouse Controller, SHALL persist across reloads, SHALL ride along in the dropped item when the controller is dismantled with the carrot, and SHALL apply to the whole warehouse. There SHALL be at most one rule per item; setting a rule for an item that has one replaces it.

#### Scenario: Reload
- **WHEN** a rule "keep 8 to 16 sticks" is set and the world is reloaded
- **THEN** the rule is still there

#### Scenario: Replace
- **WHEN** a rule for sticks exists and the player saves 32 to 64 for sticks
- **THEN** the sticks rule reads 32 to 64

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

### Requirement: Rule editing in the terminal
The Counter Terminal's amount prompt SHALL offer a Rule mode with minimum and maximum fields, Save and Remove. Save SHALL set the rule for the clicked item; Remove SHALL delete it. Items with a rule SHALL be marked in the stock grid and their tooltip SHALL show the rule and its state.

#### Scenario: Set a rule
- **WHEN** the player clicks sticks, switches to Rule, enters 8 and 16 and presses Save
- **THEN** the sticks rule exists and the sticks entry shows a marker and "keep 8 to 16" in its tooltip

#### Scenario: Remove a rule
- **WHEN** the player opens the prompt for sticks in Rule mode and presses Remove
- **THEN** the sticks rule is gone

### Requirement: Rule commands
`/servus_warehouse rules <pos>` SHALL list the rules of the network at the position with their state. `/servus_warehouse rule <pos> <item> <min> <max>` SHALL set a rule and `/servus_warehouse unrule <pos> <item>` SHALL remove one.

#### Scenario: Op sets a rule
- **WHEN** an op runs the rule command for sticks 8 16 and then the rules command
- **THEN** chat lists "Stick: keep 8 to 16" with its state
