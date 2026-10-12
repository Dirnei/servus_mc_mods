## REMOVED Requirements

### Requirement: Wireless Packing Station joins by rail
**Reason**: The Wireless Packing Station block is removed. The Warehouse Controller with a Wireless Upgrade is the site's antenna, and normal Packing Stations pack and unpack for the wireless line.
**Migration**: See `storage/warehouse-controller` (Wireless Upgrade slot, wireless capable controller) and `storage/packing-station` (Packing Stations carry wireless transfers).

### Requirement: Wireless line per team and dimension
**Reason**: The wireless line now runs between wireless capable controllers, not between stations.
**Migration**: See `storage/warehouse-controller`, requirement "Transfers between sites".

### Requirement: Transfers between sites
**Reason**: Transfers now go from a Packing Station of one site to a Packing Station of another, over the controllers.
**Migration**: See `storage/warehouse-controller`, requirement "Transfers between sites", and `storage/packing-station`.

### Requirement: Energy per item and distance
**Reason**: The FE buffer and the cost move to the Warehouse Controller.
**Migration**: See `storage/warehouse-controller`, requirements "Controller FE buffer" and "Wireless cost per item and distance".

### Requirement: Wireless rate
**Reason**: Throughput comes from the Packing Stations that send and receive.
**Migration**: See `storage/packing-station`, requirement "Wireless rate is the Packing Stations' rate".

### Requirement: Only a powered station is on the wireless line
**Reason**: Power is now a property of the controller.
**Migration**: See `storage/warehouse-controller`, requirement "Wireless capable controller".

### Requirement: Wireless Packing Station GUI
**Reason**: The block is removed; the Packing Station GUI names wireless transfers.
**Migration**: See `storage/packing-station`, requirement "Read-only buffer GUI".

### Requirement: Wireless Packing Station is obtainable
**Reason**: The block is removed; the Wireless Upgrade is crafted instead.
**Migration**: See `storage/warehouse-controller`, requirement "Wireless Upgrade is obtainable".
