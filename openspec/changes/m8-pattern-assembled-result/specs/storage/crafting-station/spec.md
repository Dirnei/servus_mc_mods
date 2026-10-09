# Spec Delta

## ADDED Requirements

### Requirement: Outputs keep a station busy
A Crafting Station whose output slots still hold items SHALL NOT be idle for jobs of anyone but the owner of its current job: it SHALL refuse a new job, and a Crafting CPU SHALL NOT give it a step but use another free station. Its own current job's outputs SHALL still be collected as usual, and once its outputs are empty it SHALL be idle again. Leftovers without any job SHALL keep it closed until a player or automation takes them out; the station's own push of outputs without a job still applies. The Production Terminal's Machines tab SHALL show such a station as blocked with "outputs not collected" instead of idle.

#### Scenario: Leftovers skip the station
- **WHEN** one of two Crafting Stations on a line holds cobblestone in its outputs and no job, and a sticks order starts
- **THEN** the sticks step runs on the other station, the first one gets no job and the Machines tab shows it blocked

#### Scenario: Own outputs still collected
- **WHEN** a station finishes the crafts of its job and its outputs wait for the CPU
- **THEN** the CPU collects them as before and the station is idle once they are gone
