## ADDED Requirements

### Requirement: Water by bucket
Using an item that holds or takes fluid on a Field Head SHALL try to fill the head's buffer from it, the way a Fluid Tank is filled: water goes in as far as the buffer has room and the item gives up what was taken (a water bucket becomes an empty bucket). A player in creative mode SHALL keep the item unchanged. A fluid item that gives no water to the head (an empty bucket, a lava bucket, or any item while the buffer is full) SHALL change nothing and SHALL NOT open the GUI. The head SHALL never fill an item from its buffer. Every other item and the empty hand SHALL open the GUI as before.

#### Scenario: Water bucket fills the head
- **WHEN** a player uses a water bucket on a Field Head with an empty buffer
- **THEN** the buffer holds 1000 mB more and the player holds an empty bucket

#### Scenario: Lava bucket refused
- **WHEN** a player uses a lava bucket on a Field Head
- **THEN** the buffer is unchanged, the player still holds the lava bucket and no GUI opens

#### Scenario: Empty bucket gets nothing
- **WHEN** a player uses an empty bucket on a Field Head holding water
- **THEN** the buffer is unchanged and the player still holds the empty bucket

#### Scenario: Full head keeps the bucket
- **WHEN** a player uses a water bucket on a Field Head whose buffer is full
- **THEN** the buffer is unchanged and the player still holds the water bucket

#### Scenario: Creative keeps the bucket
- **WHEN** a player in creative mode uses a water bucket on a Field Head with room
- **THEN** the buffer holds 1000 mB more and the player still holds the water bucket

#### Scenario: Other items open the GUI
- **WHEN** a player uses a wheat seed on a Field Head
- **THEN** the head's GUI opens
