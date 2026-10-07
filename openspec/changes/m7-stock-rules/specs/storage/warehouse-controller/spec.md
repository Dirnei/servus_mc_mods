# Spec Delta

## ADDED Requirements

### Requirement: Controller holds the stock rules
The controller SHALL store the warehouse's stock rules, keep them across reloads, hand them to whichever network it belongs to, and keep them in the dropped item when dismantled with the Configuration Carrot.

#### Scenario: Carrot dismantle
- **WHEN** a controller with a sticks rule is dismantled with the carrot and placed again
- **THEN** the sticks rule is still there

### Requirement: Rule commands
`/servus_warehouse rules <pos>`, `/servus_warehouse rule <pos> <item> <min> <max>` and `/servus_warehouse unrule <pos> <item>` SHALL list, set and remove stock rules of the network at the position.

#### Scenario: Op lists rules
- **WHEN** an op runs the rules command on a warehouse with a sticks rule
- **THEN** chat lists the rule with its state
