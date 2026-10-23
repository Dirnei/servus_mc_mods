## MODIFIED Requirements

### Requirement: Station claims survive a reload
An Inbound Station buffer slot taken by a put-away job SHALL stay locked for that job across a reload (see `shuttles/slot-locks`), so the stack is not planned a second time. While the warehouse is online and every chunk of its site is loaded, a lock whose job is neither queued nor on a shuttle at two checks in a row SHALL be released and its stack planned again. Checks run every few seconds.

#### Scenario: Claim kept
- **WHEN** a put-away job for a station slot is queued and the station and controller reload
- **THEN** the slot stays locked, no second job is made for it, and the queued job puts the stack away

#### Scenario: Claim without a job
- **WHEN** a station loads with a slot locked by a job that no longer exists
- **THEN** after a few seconds the lock is released and the stack is put away by a new job

### Requirement: A restore that cannot go on ends in the garage
A restored shuttle whose path is no longer whole SHALL look for a new way from where it stands to the goal of its leg, else drive home. Cargo whose goal is gone SHALL be routed again like a leftover. Without a way home, or off the rails, the shuttle SHALL be put back into its garage and park its cargo there as a shuttle with cargo that has nowhere to go (see `shuttles/shuttle-garage`); jobs not loaded yet are queued again. Field shuttles end their run as before, their cargo dropping at the garage. No cargo SHALL vanish.

#### Scenario: Rack gone
- **WHEN** a shuttle carrying cobblestone to a rack is saved, the rack's rail is broken while the chunk is unloaded and the garage loads again
- **THEN** the shuttle routes the cobblestone to another rack or parks it in its garage, and nothing drops
