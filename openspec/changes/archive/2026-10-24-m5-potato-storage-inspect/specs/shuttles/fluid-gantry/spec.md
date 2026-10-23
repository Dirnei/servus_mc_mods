# Spec Delta

## ADDED Requirements

### Requirement: Gantry inspect
Using the Data Potato on any part of a Fluid Gantry SHALL print a chat report: the gantry's mode, then one line per tank (left, then right) with its fluid, amount and capacity in mB, or that it is empty with its capacity, and its lock (a fluid or Any). It SHALL open no GUI and change nothing.

#### Scenario: Storage gantry report
- **WHEN** a Storage gantry with one Capacity Upgrade holds 12,000 mB of water in its left tank locked to water and an empty right tank on Any, and a player uses the potato on its beam
- **THEN** the chat shows Storage, left: water 12,000 of 32,000 mB, locked to water, and right: empty, 32,000 mB, Any

#### Scenario: Nothing opens
- **WHEN** a player uses the potato on a leg of a gantry
- **THEN** the report is printed, no GUI opens and the tanks, locks and mode are unchanged

## MODIFIED Requirements

### Requirement: Gantry GUI
Right clicking a gantry with an empty hand or a non fluid item other than the Data Potato SHALL open a GUI with a gauge per tank showing fluid and amount against capacity, a lock button under each gauge that opens a fluid list (Any, every fluid that has a bucket, and "Lock to content"), a mode button cycling Storage, Import and Export, the capacity upgrade slots, a state line and the player inventory. The state line SHALL name the first thing that is wrong (a tank holding a fluid other than its lock, an upgrade that cannot come out) and otherwise say what the mode does. Shift clicking a Capacity Upgrade SHALL move it into the slots.

#### Scenario: Lock from the list
- **WHEN** a player opens the left lock list and picks Sulfur
- **THEN** the left tank is locked to Sulfur and its button shows Sulfur

#### Scenario: Lock to content
- **WHEN** the right tank holds Mercury on Any and the player picks "Lock to content"
- **THEN** the right tank is locked to Mercury
