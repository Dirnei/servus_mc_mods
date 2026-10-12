## ADDED Requirements

### Requirement: Record knows whether the site can trade
A site's record SHALL also keep whether its controller holds a Wireless Upgrade, whether the controller is powered for wireless, and whether the site has a Packing Station. The record SHALL be updated while the site is online when the upgrade is inserted or taken, when the powered state flips, and when the first Packing Station joins or the last one leaves, and SHALL be kept while the site sleeps. A sleeping site's controller keeps its FE, so the record stays right while it sleeps.

#### Scenario: Upgrade inserted
- **WHEN** a Wireless Upgrade goes into the controller of an online site with FE
- **THEN** the site's record says it holds the upgrade and is powered for wireless

#### Scenario: Controller loses power
- **WHEN** the controller of an online site runs out of FE and its chunks unload
- **THEN** the site's record says it is not powered for wireless, and stays so while the site sleeps

#### Scenario: Last Packing Station removed
- **WHEN** the only Packing Station of an online site is broken
- **THEN** the site's record says it has no Packing Station

## REMOVED Requirements

### Requirement: Record knows the wireless stations
**Reason**: The Wireless Packing Station is removed; whether a site can trade is now a property of its controller and its Packing Stations.
**Migration**: See "Record knows whether the site can trade" in this capability.
