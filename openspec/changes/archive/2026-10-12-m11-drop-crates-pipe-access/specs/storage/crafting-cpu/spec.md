## REMOVED Requirements

### Requirement: Pushed outputs count
**Reason**: A Packing Station registers no item capability any more, so a producer can no longer push its outputs into a Packing Station face. Outputs of a step reach storage only as packets the CPU moves.
**Migration**: The CPU collects a step's outputs as packets through the data line, as before for producers that do not push; nothing needs to be built differently.
