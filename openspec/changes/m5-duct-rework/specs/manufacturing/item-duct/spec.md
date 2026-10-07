# Spec Delta

## REMOVED Requirements

### Requirement: Ducts connect to ducts and inventories
**Reason**: The Manufacturing Item Duct is replaced by the Utility Duct's Item Pipe from Logistics.
**Migration**: Place a Utility Duct with an Item Pipe. Nothing is released, so no world migration exists.

### Requirement: Inserted items are routed to an accepting inventory
**Reason**: Covered by the Utility Duct's item line behaviour.
**Migration**: See `logistics/utility-duct`.

### Requirement: Ducts hold nothing
**Reason**: Covered by the Utility Duct, which holds no items either.
**Migration**: See `logistics/utility-duct`.

### Requirement: Throughput is data
**Reason**: The Utility Duct reads its budget from `data/servus_logistics/duct/utility_duct.json`.
**Migration**: Tune `items_per_tick` there.

### Requirement: Crafting
**Reason**: The Item Pipe is crafted in Logistics.
**Migration**: Use the Item Pipe recipe.
