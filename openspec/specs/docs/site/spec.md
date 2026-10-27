# docs/site Specification

## Purpose
The docs site is the public face of the Servus mods on GitHub Pages: it describes the released mods for players and the Foundation data API for mod developers.

## Requirements

### Requirement: Site is published to GitHub Pages
The site SHALL be built from `docs/` and deployed to GitHub Pages on every push to `main` that touches `docs/`, and on manual dispatch. The deploy SHALL NOT publish when the build fails.

#### Scenario: Docs change on main
- **WHEN** a commit on `main` changes a file under `docs/`
- **THEN** the docs workflow builds and publishes the site

#### Scenario: Broken build
- **WHEN** the site build fails
- **THEN** the workflow stops and the published site stays unchanged

### Requirement: Site describes released mods only
The site SHALL have a landing page, a getting started page, a page per released mod, a developer guide to the Foundation data API and a datapack reference. Each mod page SHALL describe the content of that mod's latest release tag, not unreleased work, and SHALL end with a mod info box giving the version, mod id, Minecraft version, required mods and a download link. Public pages SHALL work without JavaScript.

#### Scenario: Unreleased mod
- **WHEN** a mod has no release tag
- **THEN** the site has no page for it and does not name it

#### Scenario: Mod info box
- **WHEN** a reader reaches the end of the Shuttles page
- **THEN** a box lists version 0.4.0, mod id `servus_shuttles`, Minecraft 26.1.2, NeoForge and Servus: Foundation 0.4.0 or newer as requirements, and a link to the release

#### Scenario: Developer guide
- **WHEN** a mod developer opens the developer section
- **THEN** it explains the data line concepts, how a machine joins the data line with a Java example, deliveries and the Machine Adapter, as of the released Foundation version
