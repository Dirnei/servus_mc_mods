# Spec Delta

## REMOVED Requirements

### Requirement: Splitter pulls from adjacent inventories
**Reason**: A Utility Duct side set to pull in does the same.
**Migration**: Set the side's item mode to pull in in the endpoint GUI.

### Requirement: Splitter is a duct
**Reason**: The Splitter Duct block no longer exists.
**Migration**: Use a Utility Duct with an Item Pipe.

### Requirement: Obtained through research
**Reason**: The `splitter_filter_ducts` tech is removed; pulling is a base feature of the Utility Duct.
**Migration**: None.
