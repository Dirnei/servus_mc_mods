## ADDED Requirements

### Requirement: Controller shows the wake
While its site is awake by a wake, the controller's activity lamp SHALL be orange, unless the site is over capacity (red). The controller GUI SHALL show a wake line: awake for a transfer, staying awake for the grace time with the seconds left, or waiting to wake with the reason; nothing while the site is not being woken.

#### Scenario: Awake
- **WHEN** a site is woken and a player opens its controller GUI
- **THEN** the lamp is orange and the GUI says the site is awake

#### Scenario: Waiting
- **WHEN** a wake waits because the team limit is reached and a player opens the controller GUI
- **THEN** the GUI says the site is waiting to wake because the team limit is reached
