# Spec Delta

## REMOVED Requirements

### Requirement: Dock block and buffers
**Reason**: The Machine Dock is replaced by the Outbound Station (feeds) and the Inbound Station (collects).
**Migration**: None, nothing is released. Place an Outbound Station in front of the machine and an Inbound Station beside it.

### Requirement: Dock is a warehouse node without stock
**Reason**: Replaced by the Outbound Station's node requirement.
**Migration**: None.

### Requirement: Collected items are put away
**Reason**: Collection is the Inbound Station's job.
**Migration**: None.

### Requirement: Feed buffer is pushed into the machine
**Reason**: Replaced by the Outbound Station's push sides.
**Migration**: None.

### Requirement: Dock reports its producer
**Reason**: Replaced by the Outbound Station's producer reporting.
**Migration**: None.

### Requirement: Warehouse feeds a dock
**Reason**: Replaced by the Outbound Station's deliveries on request.
**Migration**: None.

### Requirement: Dock GUI
**Reason**: Replaced by the Outbound Station GUI.
**Migration**: None.

### Requirement: Dock is obtainable and dismantled
**Reason**: The block no longer exists.
**Migration**: None.
