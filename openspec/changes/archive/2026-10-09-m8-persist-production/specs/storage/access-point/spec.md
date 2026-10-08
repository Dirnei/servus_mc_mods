# Spec Delta

## ADDED Requirements

### Requirement: Shipments survive a reload
An Access Point SHALL save its open shipments with their ids, targets, items and amounts, and their progress SHALL be kept by id so that whoever waits for a shipment can book it after a reload. After its block entity loads again, a shipment SHALL only wait for what the outgoing buffer holds for it; the rest SHALL end as a shortfall, so its owner ships it again. Items that arrive later for no open shipment SHALL be put away as before.

#### Scenario: Restart while items wait in the outgoing buffer
- **WHEN** a shipment of 16 planks has 8 planks in the outgoing buffer and 8 still on a shuttle when the server restarts
- **THEN** after the restart the shipment sends the 8 planks, ends 8 short, and its requester ships the 8 again
