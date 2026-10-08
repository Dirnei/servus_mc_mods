# Spec Delta

## ADDED Requirements

### Requirement: Requests survive a reload
The open requests of a Counter Terminal SHALL be saved with the terminal and SHALL be there again after the world is reloaded or the terminal's chunk loads again: item, requested, delivered and missing amounts, their shipments and the CPU holding their work order. Request ids SHALL stay unique across terminals and reloads. After a reload a request SHALL go on as before: shipments that still run are booked when they end, a missing part is shipped again, and a request whose work order no longer exists is retried as missing stock.

#### Scenario: Restart with a producing request
- **WHEN** a request for 8 sticks is producing on a CPU and the server restarts
- **THEN** the request is listed again after the restart and the sticks land in the terminal once the order is done

#### Scenario: Ids stay unique
- **WHEN** a terminal with request 5 is reloaded and a new request is made
- **THEN** the new request gets an id above every id used before
