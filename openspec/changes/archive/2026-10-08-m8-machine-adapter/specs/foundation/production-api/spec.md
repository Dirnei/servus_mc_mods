# Spec Delta

## ADDED Requirements

### Requirement: Supplier capability
Foundation SHALL define a sided block capability `servus_foundation:supplier`. A supplier SHALL accept a request to send up to an amount of items like a given stack to a data member position and SHALL answer how many it will send, zero when it cannot. The items SHALL travel by the supplier's own means, for example as packets over the data line.

#### Scenario: Supplier answers
- **WHEN** a mod asks a supplier holding 30 coal to send 16 coal to a data member it reaches
- **THEN** the answer is 16 and 16 coal arrive at the member later

#### Scenario: Nothing to send
- **WHEN** a mod asks a supplier without coal to send coal
- **THEN** the answer is 0
