## REMOVED Requirements

### Requirement: Wake command
**Reason**: The command moves under `/servus shuttles` with warehouse and machine numbers instead of block positions.
**Migration**: Use the matching command in `shuttles/commands` (`wake`, and `list` in place of `sites`).
