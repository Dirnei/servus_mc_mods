# Spec Delta

## REMOVED Requirements

### Requirement: Order Board joins the warehouse
**Reason**: The Order Board showed orders of the controller and Outbound Stations; orders now live on Crafting CPUs and the Production Terminal replaces the board.
**Migration**: Use `/servus_warehouse orders <pos>` and the CPU summary click until `m8-production-terminal`.

### Requirement: Machines section
**Reason**: Removed with the Order Board.
**Migration**: `/servus_warehouse producers <pos>` lists the producers of the data line.

### Requirement: Orders section
**Reason**: Removed with the Order Board.
**Migration**: `/servus_warehouse orders <pos>`.

### Requirement: Cancel from the board
**Reason**: Removed with the Order Board.
**Migration**: Cancel the terminal request of the order.

### Requirement: Live view
**Reason**: Removed with the Order Board.
**Migration**: None until the Production Terminal.
