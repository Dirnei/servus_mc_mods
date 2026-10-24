## REMOVED Requirements

### Requirement: Several warehouses on one line
**Reason**: A data line belongs to at most one warehouse; a line that reaches two is in conflict (see `shuttles/line-warehouse`). Outposts are reached through wireless sites and the terminal's site selector.
**Migration**: Split the line so each warehouse has its own, and use the site selector of a wireless capable controller to request from another site.
