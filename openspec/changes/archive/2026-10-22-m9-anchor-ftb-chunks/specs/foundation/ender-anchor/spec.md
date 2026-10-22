## MODIFIED Requirements

### Requirement: FTB Chunks respected
Ender Anchors SHALL work normally when FTB Chunks is installed, since the server config `enderAnchorRespectsFtbChunks` is false by default. When FTB Chunks is installed and `enderAnchorRespectsFtbChunks` is true, Ender Anchors SHALL stay inactive, load no chunk and show a status pointing to FTB Chunks.

#### Scenario: FTB Chunks present
- **WHEN** FTB Chunks is installed with the default config and a powered, owned Ender Anchor is placed
- **THEN** the anchor is active and loads its square

#### Scenario: Option on
- **WHEN** FTB Chunks is installed, `enderAnchorRespectsFtbChunks` is true and a powered, owned Ender Anchor is placed
- **THEN** the anchor is inactive, loads no chunk and its GUI says to use FTB Chunks

#### Scenario: Option on without FTB Chunks
- **WHEN** FTB Chunks is not installed and `enderAnchorRespectsFtbChunks` is true
- **THEN** a powered, owned Ender Anchor is active as usual
