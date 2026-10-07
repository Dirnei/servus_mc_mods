# Proposal

## Why

Play test (2026-10-07): a two line duct looks right only when it runs north to south. Its centre cube always paints the two colour bands along the north south axis, so on an east to west run the bands on the centre cross the bands on the arms.

## What Changes

- The two line centre follows the run: when the duct's arms lie only on the east west axis the centre paints its bands along that axis. Mixed junctions and vertical or north south runs keep the north south centre.

## Capabilities

### Modified Capabilities
- `logistics/duct-appearance`: the bands of the centre run with the arms.

## Non-goals

- A seamless junction where arms meet from both horizontal axes.

## Impact

- `logistics`: a second two line centre part `center_both_x`, generated, registered and chosen by the dynamic model from the arm directions.
