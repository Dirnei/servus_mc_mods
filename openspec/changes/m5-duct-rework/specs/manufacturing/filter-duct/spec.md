# Spec Delta

## REMOVED Requirements

### Requirement: Whitelist set in the GUI
**Reason**: The Utility Duct has a filter per side in its endpoint GUI.
**Migration**: Set the filter on the side that touches the target inventory.

### Requirement: Only listed items are delivered through the filter
**Reason**: Covered by the Utility Duct's per side item filters.
**Migration**: See `logistics/utility-duct`.

### Requirement: Obtained through research
**Reason**: The `splitter_filter_ducts` tech is removed; filters are a base feature of the Utility Duct.
**Migration**: None.
