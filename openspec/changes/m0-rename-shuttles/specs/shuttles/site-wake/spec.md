# Spec Delta

## MODIFIED Requirements

### Requirement: Waking costs FE per chunk
While a wake holds a site loaded, awake or in its grace time, the woken site's controller SHALL pay `wake_fe_per_chunk_per_tick` (example 2, `shuttles/power.json`) for every chunk of the site each tick. A site loaded by players or other chunk loaders without a wake SHALL pay nothing for being loaded. When a tick of a wake cannot be paid, the wake SHALL end at once and release every chunk it loaded, with the reason no power. A refused draw of another consumer (a shuttle, a Crafting Station) SHALL NOT end the wake.

#### Scenario: Nine chunks
- **WHEN** a site of 9 chunks is woken for 30 seconds with 2 FE per chunk and tick
- **THEN** its controller pays 18 FE per tick, 10,800 FE in total

#### Scenario: Runs dry while awake
- **WHEN** a woken site's controller cannot pay the next tick
- **THEN** the site's chunks are released and its wake line says no power
