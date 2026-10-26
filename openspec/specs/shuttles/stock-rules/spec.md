# shuttles/stock-rules Specification

## Purpose

Stock rules keep a warehouse stocked on its own: an item with a minimum and a maximum is reordered whenever it runs low.

## Requirements

### Requirement: Rules are kept on the warehouse
A stock rule SHALL name one item and hold a Make pair, a cap, or both. The Make pair is a minimum and a maximum with minimum below maximum; the cap is an amount of at least the maximum when both are set. A rule SHALL have at least one of the two. Rules SHALL be stored on the Warehouse Controller, SHALL persist across reloads, SHALL ride along in the dropped item when the controller is dismantled with the carrot, and SHALL apply to the whole warehouse. There SHALL be at most one rule per item; setting a rule for an item that has one replaces it. Rules saved before caps existed SHALL load as rules with their Make pair and no cap.

#### Scenario: Reload
- **WHEN** a rule "keep 8 to 16 sticks" is set and the world is reloaded
- **THEN** the rule is still there

#### Scenario: Replace
- **WHEN** a rule for sticks exists and the player saves 32 to 64 for sticks
- **THEN** the sticks rule reads 32 to 64

#### Scenario: Cap only
- **WHEN** the player saves a rule for cobblestone with no Make pair and a cap of 4096
- **THEN** the cobblestone rule exists with the cap 4096 and orders nothing

#### Scenario: Nothing set
- **WHEN** the player tries to save a rule with neither a Make pair nor a cap
- **THEN** the rule is not saved and the prompt says a rule needs a Make pair or a cap

#### Scenario: Cap below the maximum
- **WHEN** the player tries to save sticks 8 to 16 with a cap of 10
- **THEN** the rule is not saved and the prompt says the cap must not be below Fill up to

#### Scenario: Old world
- **WHEN** a world saved before caps holds the rule "keep 8 to 16 sticks"
- **THEN** it loads as sticks 8 to 16 without a cap

### Requirement: Rules order when stock runs low
Every `stock_rule_interval` ticks (example 100, from `warehouse.json`) the warehouse SHALL check each rule that has a Make pair: when the indexed count of the item plus the amount of its open work orders on the CPUs of the controller's data line is below the minimum, a CPU of that line SHALL get a work order for the difference between the maximum and that sum. A rule without a Make pair SHALL never order. A rule whose controller reaches no CPU (neither the controller nor a Packing Station of the warehouse on a data line with a CPU) SHALL show "no CPU". A rule whose item no producer of the line can make SHALL NOT create an order and SHALL show "no producer" as its state; a rule whose item is above the minimum SHALL show "ok"; a rule with an open order SHALL show "ordering".

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

#### Scenario: Cap only rule with no stock
- **WHEN** a rule for cobblestone has only a cap and the warehouse holds no cobblestone
- **THEN** no order is created and the rule shows ok

### Requirement: Rule editing in the terminal
The Storage Terminal's item popup SHALL have a Rule tab with an "Order when below" (the minimum) and a "Fill up to" (the maximum) one line stepper, Save and Remove. Save SHALL set the rule for the popup's item; Remove SHALL delete it and SHALL be greyed out while the item has no rule. Save SHALL be greyed out, with the reason as its tooltip and in the tab, while the minimum is not below the maximum. Items with a rule SHALL be marked in the stock grid and their tooltip SHALL show the rule and its state.

#### Scenario: Set a rule
- **WHEN** the player right clicks sticks, picks the Rule tab, sets Order when below to 8 and Fill up to to 16 and presses Save
- **THEN** the sticks rule exists and the sticks entry shows a marker and "keep 8 to 16" in its tooltip

#### Scenario: Remove a rule
- **WHEN** the player opens the Rule tab for sticks, which have a rule, and presses Remove
- **THEN** the sticks rule is gone

#### Scenario: Minimum not below maximum
- **WHEN** the Rule tab shows Order when below 16 and Fill up to 16
- **THEN** Save is greyed out and the tab says the minimum must be below Fill up to

#### Scenario: Nothing to remove
- **WHEN** the player opens the Rule tab for an item without a rule
- **THEN** Remove is greyed out and the steppers show 0 and one stack of the item

### Requirement: Rule commands
`/servus_warehouse rules <pos>` SHALL list the rules of the network at the position with their Make pair, cap and state. `/servus_warehouse rule <pos> <item> <min> <max>` SHALL set the Make pair of a rule and keep its cap. `/servus_warehouse cap <pos> <item> <amount>` SHALL set the cap of a rule and keep its Make pair. `/servus_warehouse uncap <pos> <item>` SHALL clear the cap and remove the rule when no Make pair is left. `/servus_warehouse unrule <pos> <item>` SHALL remove the whole rule. A command that would break the threshold rules SHALL fail with the reason.

#### Scenario: Op sets a rule
- **WHEN** an op runs the rule command for sticks 8 16 and then the rules command
- **THEN** chat lists "Stick: keep 8 to 16" with its state

#### Scenario: Op sets a cap
- **WHEN** an op runs the cap command for cobblestone 4096 and then the rules command
- **THEN** chat lists "Cobblestone: refuse above 4096" with its state

#### Scenario: Uncap a cap only rule
- **WHEN** cobblestone has only a cap and an op runs the uncap command for cobblestone
- **THEN** the cobblestone rule is gone

#### Scenario: Cap below the maximum
- **WHEN** sticks keep 8 to 16 and an op runs the cap command for sticks 10
- **THEN** the command fails saying the cap must not be below the maximum

### Requirement: Refusing state
A rule SHALL show the state "refusing" while any Inbound Station of its warehouse refused part of that item because of the cap since the rule's previous check, naming the first station that refused by its label (see `shuttles/station-numbers`), for example "refused at Inbound #1". The refusing state SHALL take precedence over the other states and SHALL clear at the first check after which no station refused.

#### Scenario: Quarry at the cap
- **WHEN** cobblestone has a cap of 4096, the warehouse holds 4096 and a hopper keeps offering cobblestone to Inbound #1
- **THEN** within one interval the rule reads "refused at Inbound #1"

#### Scenario: Below the cap again
- **WHEN** the cobblestone count falls below the cap and no station refuses for a whole interval
- **THEN** the rule shows ok

### Requirement: Cap editing in the terminal
The Rule tab of the Storage Terminal item popup SHALL show a Limit group beside the Make group with a "Refuse at Inbound above" field that can be off or hold an amount, stepped by its minus and plus buttons and typed into. Save SHALL store the Make pair and the cap together, refusing with a message in the popup when the threshold rules are broken. The item's tooltip in the stock grid SHALL show the cap, for example "refuse above 4096".

#### Scenario: Set a cap
- **WHEN** the player opens the Rule tab for cobblestone, leaves Make off, types 4096 into Refuse at Inbound above and presses Save
- **THEN** the cobblestone rule has the cap 4096 and its tooltip shows "refuse above 4096"

#### Scenario: Turn a cap off
- **WHEN** sticks keep 8 to 16 with a cap of 1000 and the player steps the cap field down to off and presses Save
- **THEN** the sticks rule keeps 8 to 16 and has no cap

### Requirement: Refuse column in the Rules tab
The terminal Rules tab SHALL show each rule's cap in a Refuse column, a dash for a rule without a cap, and SHALL show the refusing state with the station label in the state column.

#### Scenario: Rules list
- **WHEN** sticks keep 8 to 16 and cobblestone has only a cap of 4096 that Inbound #1 is refusing
- **THEN** the Rules tab shows sticks with a dash under Refuse and cobblestone with 4096 under Refuse and "refused at Inbound #1"
