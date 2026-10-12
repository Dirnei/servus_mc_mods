## ADDED Requirements

### Requirement: Waking costs FE per chunk
While a wake holds a site loaded, awake or in its grace time, the woken site's controller SHALL pay `wake_fe_per_chunk_per_tick` (example 2, `storage/power.json`) for every chunk of the site each tick. A site loaded by players or other chunk loaders without a wake SHALL pay nothing for being loaded. When a tick of a wake cannot be paid, or the woken site's controller is in the no power state, the wake SHALL end at once and release every chunk it loaded, with the reason no power.

#### Scenario: Nine chunks
- **WHEN** a site of 9 chunks is woken for 30 seconds with 2 FE per chunk and tick
- **THEN** its controller pays 18 FE per tick, 10,800 FE in total

#### Scenario: Runs dry while awake
- **WHEN** a woken site's controller cannot pay the next tick
- **THEN** the site's chunks are released and its wake line says no power

### Requirement: No wake without power
A site SHALL NOT wake unless its controller holds at least the FE for one second awake: chunks times `wake_fe_per_chunk_per_tick` times 20. For a sleeping site the FE SHALL be taken from its record; a request SHALL then wait with the reason "no power". The site record SHALL keep the FE its controller held when its chunk unloaded, refreshed with the stock snapshot while online.

#### Scenario: Not enough stored
- **WHEN** a site of 9 chunks sleeps with 100 FE in its controller and 2 FE per chunk and tick
- **THEN** a wake is refused with the reason no power and no chunk is loaded

#### Scenario: Enough stored
- **WHEN** the same site slept with 5,000 FE
- **THEN** it wakes and its chunks load
