## MODIFIED Requirements

### Requirement: A gift speeds up the gnome
A bound gnome that has taken a gift SHALL dig at the rate its vein and feed give it, times the gift's factor, until the gift's duration has run out, then at the rate without the gift again. A gift SHALL never be needed: without one the gnome SHALL work at the rate its feed gives. The gift and the feed SHALL multiply: a gift SHALL neither replace the feed nor change how much each dug item uses. A new gift while a boost runs SHALL replace it, starting the new gift's full duration with its factor; boosts SHALL NOT stack. The remaining time SHALL count down on every tick the Hollow ticks (while its chunk is loaded), also while the store is full.

#### Scenario: Faster with a gift
- **WHEN** a bound, unfed iron Hollow with a yield time of 1,200 ticks takes a cookie (x2) and runs for 2,400 ticks
- **THEN** its store holds 4 raw iron, where it would hold 2 without the gift

#### Scenario: Gift on a fed gnome
- **WHEN** a bound iron Hollow with all three principles fed (x3) and a yield time of 1,200 ticks takes a cookie (x2)
- **THEN** it digs one raw iron every 200 ticks, and each one uses the same principles as without the gift

#### Scenario: Boost runs out
- **WHEN** the gift's duration has passed
- **THEN** the gnome digs at the rate without the gift again and the Hollow shows no boost

#### Scenario: Refresh
- **WHEN** a gnome with 100 ticks of boost left takes another cookie
- **THEN** its boost has the full 12,000 ticks left, not 12,100
