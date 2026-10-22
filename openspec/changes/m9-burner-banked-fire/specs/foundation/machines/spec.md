## ADDED Requirements

### Requirement: Burner machines bank their fire
A burner machine SHALL count its burn down only on ticks it works. While it has no work, a burn in progress SHALL pause with its remaining time kept, so no fuel is wasted; when work comes back, the paused burn SHALL go on before a new fuel item is consumed. A new fuel item SHALL be consumed only when the previous burn has ended and work is available. The remaining burn SHALL be kept when the world is saved and loaded. A machine with an eternal fire SHALL work without fuel as before.

#### Scenario: No work pauses the burn
- **WHEN** a burner machine that has just lit a coal loses its work, for example because its output is full
- **THEN** its remaining burn time stays the same for as long as it has no work, and no new fuel is taken

#### Scenario: Work again
- **WHEN** the paused machine gets work again
- **THEN** it works on the paused burn and takes the next fuel item only when that burn has ended

### Requirement: Restart threshold
A machine with a buffer it fills (an FE buffer or a water tank) MAY have a restart threshold `restart_below` in its machine stats, a fraction of the buffer's capacity between 0 and 1. Once its buffer is full, which means it has no room left for one more work cycle (one tick of FE, one `fluid_per_cycle` of water), such a machine SHALL stay off until the buffer holds less than `restart_below` times its capacity, and SHALL then run until the buffer is full again. With `restart_below` 1, or without the field on a machine that has no threshold of its own, the machine SHALL run again as soon as the buffer has room. The waiting state SHALL be kept when the world is saved and loaded.

#### Scenario: Waits after full
- **WHEN** a machine with `restart_below` 0.5 fills its buffer and a consumer then takes a little
- **THEN** the machine stays off while the buffer holds half or more

#### Scenario: Restarts below the threshold
- **WHEN** the buffer of that machine drops below half
- **THEN** the machine runs again and keeps running until the buffer is full

#### Scenario: Waiting is visible
- **WHEN** a machine with a restart threshold waits with its buffer above the threshold
- **THEN** it is not lit, and its GUI tells that it waits and below which amount it restarts

#### Scenario: Retuned threshold
- **WHEN** a datapack sets `restart_below` of `servus_foundation:coal_generator` to 0.9 and the server reloads
- **THEN** the generator restarts once its buffer drops below 90 percent

## MODIFIED Requirements

### Requirement: Lit state
A machine SHALL show a lit front while it is working and an unlit front otherwise. A burner machine whose burn is paused because it has no work SHALL show as not lit, and its lit state SHALL change only when it starts or stops working, not while it keeps working or keeps waiting.

#### Scenario: Working
- **WHEN** a burner machine is burning fuel and processing
- **THEN** its front texture is the lit variant

#### Scenario: Paused burn
- **WHEN** a burner machine with a burn in progress has no work
- **THEN** its front texture is the unlit variant and its GUI shows no flame
