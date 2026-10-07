# Spec Delta

## REMOVED Requirements

### Requirement: Cables connect visually and logically
**Reason**: The Manufacturing Power Cable is replaced by the Utility Duct's Power Cable from Logistics.
**Migration**: Place a Utility Duct with a Power Cable. Nothing is released, so no world migration exists.

### Requirement: Connected cables form one network
**Reason**: Covered by the Utility Duct's energy line networks.
**Migration**: See `logistics/utility-duct`.

### Requirement: Networks move energy every tick
**Reason**: Covered by the Utility Duct's energy line behaviour.
**Migration**: See `logistics/utility-duct`.

### Requirement: Cables carry no buffer
**Reason**: Covered by the Utility Duct, which buffers no energy either.
**Migration**: See `logistics/utility-duct`.

### Requirement: Crafting
**Reason**: The Power Cable is crafted in Logistics.
**Migration**: Use the Power Cable recipe.
