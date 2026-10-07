# Spec Delta

## MODIFIED Requirements

### Requirement: Producer and feed commands
`/servus_warehouse producers <pos>` SHALL list every Outbound Station of the network at the position with the block it faces and that block's recipes. `/servus_warehouse feed <station> <item> <count>` SHALL ask the warehouse to deliver the items into that Outbound Station's buffer and SHALL answer with the refusal reason when it cannot.

#### Scenario: Op lists producers
- **WHEN** an op runs the producers command on a crate of a warehouse with one Outbound Station facing an Assembler
- **THEN** chat lists that station with the Assembler's recipes

#### Scenario: Op feeds a dock
- **WHEN** an op runs the feed command for 4 iron ingots on an Outbound Station while a crate holds 10
- **THEN** 4 ingots arrive in the station's buffer
