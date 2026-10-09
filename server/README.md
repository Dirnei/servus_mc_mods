# Play test server

A NeoForge 1.21.1 server in Docker with the Servus mods and the same helper mods as the dev runs
(Modonomicon, JEI, Jade, FTB Ultimine with FTB Library and Architectury).

## Host

```powershell
.\server\pack.ps1                      # build, fill server\mods, write server\dist\*.mrpack
copy server\.env.example server\.env   # once: set OPS and WHITELIST
cd server; docker compose up -d
docker compose logs -f                 # wait for "Done"
```

After a code change: run `pack.ps1` again, then `docker compose restart`. The world lives in
`server\data` and survives restarts. Console: `docker attach servus-playtest`, leave with Ctrl+P Ctrl+Q.

## Players

1. Install [Prism Launcher](https://prismlauncher.org) and log in with the Microsoft account.
2. Add Instance, Import, pick `servus-playtest-<version>.mrpack`. Prism installs Minecraft 1.21.1,
   NeoForge and all mods.
3. Give the instance 4 GB of memory (Edit, Settings, Java), start it, Multiplayer, add the server.

For a new pack version, delete the old instance (or its `mods` folder) and import the new pack.
Worlds are only on the server, so nothing is lost.

## Address

- Same network: the host's LAN IP, for example `192.168.1.20`.
- Elsewhere: forward TCP 25565 on the router, or put both machines on Tailscale and use the
  host's Tailscale IP.
