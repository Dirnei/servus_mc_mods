# Spec Delta

## ADDED Requirements

### Requirement: Item pipe fills partial stacks first
An Item Pipe delivering into an inventory SHALL put items into slots that already hold the item before it uses an empty slot, so a trickle builds whole stacks.

#### Scenario: Chest with a partial stack
- **WHEN** a pipe delivers 8 cobblestone into a chest whose first slot is empty and whose third slot holds 20 cobblestone
- **THEN** the third slot holds 28 cobblestone and the first slot stays empty

#### Scenario: Partial stack full
- **WHEN** the third slot holds 60 cobblestone and the pipe delivers 8
- **THEN** 4 go into the third slot and 4 into the first empty slot

### Requirement: Item pipe announces its pulls
Before an Item Pipe sends pulled items to a block that answers offers (a packet endpoint), it SHALL offer that block what its pulling source can give of the item, after the side's keep amount and up to a stack, and SHALL send that item to that block only into the grant it gets. A refused offer SHALL leave the pipe sending as to any inventory. The pipe SHALL move no more items per pull and per tick than without grants.

#### Scenario: Station told about a stack
- **WHEN** a pipe pulls from a chest with 64 cobblestone toward an Inbound Station
- **THEN** the station is offered 64 cobblestone before the first item moves, and the pipe still moves `items_per_pull` items every `pull_interval` ticks

#### Scenario: Keep amount respected
- **WHEN** the pulling side keeps 16 cobblestone and the chest holds 50
- **THEN** the offer is for 34 cobblestone

#### Scenario: Plain chest on the same channel
- **WHEN** the pipe's channel also reaches a chest at the same priority as the station
- **THEN** the chest gets its share as before and the station's share goes into the grant

#### Scenario: Refused offer
- **WHEN** the station refuses because its buffer is full
- **THEN** the pipe sends cobblestone into whatever room the station's slots still take, as before

#### Scenario: Pushed items are not announced
- **WHEN** a machine pushes 8 cobblestone into the duct toward the station
- **THEN** no offer is made and the cobblestone goes in as before

### Requirement: Item pipe grant lifetime
An Item Pipe SHALL keep one grant per receiving side and item, offer again once the grant is filled, and end a grant when its sources no longer give the item, the side or channel changes, or the duct is broken.

#### Scenario: Next stack
- **WHEN** the chest held 100 cobblestone and the first grant of 64 is filled
- **THEN** the pipe offers the remaining 36 and sends them into the new grant

#### Scenario: Source dry
- **WHEN** a player takes the cobblestone out of the chest while a grant still expects 24
- **THEN** the pipe ends the grant within two pull intervals
