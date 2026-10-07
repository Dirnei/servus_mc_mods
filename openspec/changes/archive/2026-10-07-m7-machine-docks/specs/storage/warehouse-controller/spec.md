# Spec Delta

## ADDED Requirements

### Requirement: Producer and feed commands
`/servus_warehouse producers <pos>` SHALL list every Machine Dock of the network at the position with the block it faces and that block's recipes. `/servus_warehouse feed <dock> <item> <count>` SHALL ask the warehouse to deliver the items into that dock's feed buffer and SHALL answer with the refusal reason when it cannot.

#### Scenario: Op lists producers
- **WHEN** an op runs the producers command on a crate of a warehouse with one dock facing an Assembler
- **THEN** chat lists that dock with the Assembler's recipes

#### Scenario: Op feeds a dock
- **WHEN** an op runs the feed command for 4 iron ingots on a dock while a crate holds 10
- **THEN** 4 ingots arrive in the dock's feed buffer
