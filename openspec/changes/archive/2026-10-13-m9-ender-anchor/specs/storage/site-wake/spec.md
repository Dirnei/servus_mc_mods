## MODIFIED Requirements

### Requirement: Team cap and FTB Chunks
A wake SHALL need the site's team. While awake, a site SHALL count as one active Ender Anchor of its team against `maxEnderAnchorsPerTeam` of the Foundation Ender Anchor, and SHALL be released from that count when it goes back to sleep. A wake that would exceed the cap, a site without a team, and every wake while FTB Chunks is installed and `enderAnchorRespectsFtbChunks` is on SHALL wait with the reason (team limit reached, no owner, use FTB Chunks) and SHALL be retried while it is wanted.

#### Scenario: Team cap
- **WHEN** a team already has 4 active Ender Anchors with a cap of 4 and one of its sites is to be woken
- **THEN** the site does not wake and the reason is team limit reached

#### Scenario: Cap frees up
- **WHEN** one of those Ender Anchors is broken while the wake is still wanted
- **THEN** the site wakes

#### Scenario: Counted while awake
- **WHEN** a team with no Ender Anchors has one awake site
- **THEN** the team counts one active anchor, and none after the site released its chunks
