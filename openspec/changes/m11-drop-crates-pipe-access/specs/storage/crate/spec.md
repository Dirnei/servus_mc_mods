## REMOVED Requirements

### Requirement: One type, bulk capacity
**Reason**: The crate family (Crate, Double Crate, Quad Crate) is removed. Crates added little next to Heavy Duty Racks and shuttles.
**Migration**: Store items in Heavy Duty Racks served by shuttles (see `storage/heavy-duty-rack` and `storage/shuttles`). Nothing is released, so no world migration exists.

### Requirement: Hand interaction
**Reason**: The crate block is removed.
**Migration**: Heavy Duty Racks open a chest style GUI by hand (see `storage/heavy-duty-rack`).

### Requirement: Breaking rules
**Reason**: The crate block is removed.
**Migration**: Heavy Duty Racks spill their contents when broken and keep them when dismantled with the Configuration Carrot.

### Requirement: Automation access
**Reason**: The crate block is removed.
**Migration**: A Heavy Duty Rack with no rail docked to its section is a chest for hoppers and pipes (see `storage/heavy-duty-rack`).

### Requirement: Front display
**Reason**: The crate block is removed.
**Migration**: A Heavy Duty Rack shows the item types it holds the most of on its shelves.

### Requirement: Crate variants
**Reason**: The crate blocks, items and recipes are removed.
**Migration**: None needed; the Heavy Duty Rack is the storage block of every warehouse.
