## ADDED Requirements

### Requirement: Controller GUI shows warehouse numbers
The Warehouse Controller GUI SHALL show its warehouse's number in front of the name in its title, for example "W1 North Mine", and in front of every site in its site list. The number SHALL NOT be part of the name and SHALL NOT appear in other labels, reports or messages, apart from the Shuttles commands.

#### Scenario: Title
- **WHEN** a player opens the controller of W1 named "North Mine"
- **THEN** the title reads "W1 North Mine" and the name field holds "North Mine"

#### Scenario: Site list
- **WHEN** the team has W1 "North Mine" and W3 "Farm"
- **THEN** the site list shows "W1 North Mine" and "W3 Farm"

## REMOVED Requirements

### Requirement: Warehouse command
**Reason**: The command moves under `/servus shuttles` with warehouse and machine numbers instead of block positions.
**Migration**: Use the matching command in `shuttles/commands` (`info`, `take`, `zones`).

### Requirement: Producer and feed commands
**Reason**: The command moves under `/servus shuttles` with warehouse and machine numbers instead of block positions.
**Migration**: Use the matching command in `shuttles/commands` (`producers`, `feed`).

### Requirement: Order commands
**Reason**: The command moves under `/servus shuttles` with warehouse and machine numbers instead of block positions.
**Migration**: Use the matching command in `shuttles/commands` (`orders`, `order`).

### Requirement: Rule commands
**Reason**: The command moves under `/servus shuttles` with warehouse and machine numbers instead of block positions.
**Migration**: Use the matching command in `shuttles/commands` (`rules`, `rule`, `unrule`).
