## MODIFIED Requirements

### Requirement: Dripping onto the Hollow below
A Dropper standing directly on top of a Gnome Hollow SHALL, at the drip interval (example every 20 ticks), move up to the drip amount (example 25 mB) of each principle it holds into the Hollow's tank of that principle, as long as the Hollow's vein is seeded, the vein takes that principle, and the Hollow's tank has room. An unripe vein SHALL take the principles of its ripening step; a ripe vein SHALL take the principles of its feed. It SHALL move nothing into a Hollow without a vein, and no principle the vein does not take at its stage. The Hollow SHALL keep exposing no capability, and SHALL ripen and feed its gnome from dripped principles exactly as from poured ones.

#### Scenario: Ripens without buckets
- **WHEN** a Dropper holding Salt and Mercury stands on an unripe iron Hollow with empty tanks
- **THEN** Salt and Mercury drip into the Hollow and its vein grows steps

#### Scenario: Only what the vein takes
- **WHEN** a Dropper holding Sulfur stands on an unripe iron Hollow whose steps take only Salt and Mercury
- **THEN** the Sulfur stays in the Dropper

#### Scenario: Feeds a ripe vein
- **WHEN** a Dropper holding Salt, Sulfur and Mercury stands on a ripe iron Hollow whose feed names all three, with a bound gnome and empty tanks
- **THEN** all three drip into the Hollow and its gnome digs at the fed rate

#### Scenario: Ripe vein
- **WHEN** a Dropper holding Salt stands on a ripe Hollow whose vein names no feed
- **THEN** the Salt stays in the Dropper

#### Scenario: Empty Hollow
- **WHEN** a Dropper holding Salt stands on a Hollow without a vein
- **THEN** the Salt stays in the Dropper
